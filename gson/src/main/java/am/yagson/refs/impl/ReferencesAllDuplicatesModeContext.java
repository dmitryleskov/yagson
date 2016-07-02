package am.yagson.refs.impl;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

import am.yagson.ReadContext;
import am.yagson.WriteContext;
import am.yagson.refs.*;

import com.google.gson.*;
import com.google.gson.internal.bind.AdapterUtils;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import static am.yagson.refs.References.REF_ROOT;

/**
 * Used to find circular dependencies and duplicate references during the 
 * serialization.
 * <p/>
 * Neither this context, nor the Write or Read contexts are thread-safe!
 *
 * @author Andrey Mogilev
 */
public class ReferencesAllDuplicatesModeContext {

    static ReferencesPolicy policy = ReferencesPolicy.DUPLICATE_OBJECTS;

    protected Deque<String> currentPathElements = new ArrayDeque<String>();

    protected String getCurrentReference() {
        // TODO: optimize - cache last, maybe keep paths in stack etc.
        StringBuilder sb = new StringBuilder();
        for (String el : currentPathElements) {
            sb.append(el).append('.');
        }
        if (sb.length() > 0) {
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }


    class RefsWriteContext implements ReferencesWriteContext {
        protected IdentityHashMap<Object, String> references = new IdentityHashMap<Object, String>();
        protected Deque<Object> currentObjects = new ArrayDeque<Object>(); // used only for self-checks

        public RefsWriteContext(Object root) {
            init();
            if (root != null) {
                startObject(root, REF_ROOT);
            }
        }

        // inits subclasses (before constructor completion)
        protected void init() {
        }

        /**
         * Saves the object for the further possible references.
         *
         * @param value the object to be serialized next
         * @param pathElement the element corresponding to the object in the references path
         */
        protected void startObject(Object value, String pathElement) {
            currentObjects.addLast(value);
            currentPathElements.addLast(pathElement);
            references.put(value, getCurrentReference());
        }

        /**
         * Must be invoked in pair to {@link #startObject(Object, String)} which returned null, after
         * serialization of the object is completed.
         *
         * @param value the object which serialization has been completed
         */
        protected void endObject(Object value) {
            if (value != null) {
                Object last = currentObjects.pollLast();
                if (last != value) {
                    throw new IllegalStateException("Out-of-order endObject()");
                }
                currentPathElements.removeLast();
            }
        }

        protected final <T> boolean isPotentialReference(T value, TypeAdapter<T> valueTypeAdapter) {
            // avoid creating references to simple types even in "all duplicates" mode
            return value != null && !AdapterUtils.isSimpleTypeAdapter(valueTypeAdapter);
        }

        public <T> String getReferenceFor(T value, TypeAdapter<T> valueTypeAdapter, String pathElement) {
            if (!isPotentialReference(value, valueTypeAdapter)) {
                return null;
            }

            return references.get(value);
        }

        public <T> JsonElement doToJsonTree(T value, TypeAdapter<T> valueTypeAdapter, String pathElement,
                                            WriteContext ctx) {
            String ref = getReferenceFor(value, valueTypeAdapter, pathElement);
            if (ref != null) {
                return makeReferenceElement(ref);
            } else if (isPotentialReference(value, valueTypeAdapter)) {
                startObject(value, pathElement);
                JsonElement el = valueTypeAdapter.toJsonTree(value, ctx);
                endObject(value);
                return el;
            } else {
                return valueTypeAdapter.toJsonTree(value, ctx);
            }
        }

        public <T> void doWrite(T value, TypeAdapter<T> valueTypeAdapter, String pathElement, JsonWriter out,
                                WriteContext ctx) throws IOException {

            // resolve required for correct check of isSimple (i.e. if isPotentialReference)
            valueTypeAdapter = AdapterUtils.resolve(valueTypeAdapter, value);

            String ref = getReferenceFor(value, valueTypeAdapter, pathElement);
            if (ref != null) {
                out.value(ref);
            } else if (isPotentialReference(value, valueTypeAdapter)) {
                startObject(value, pathElement);
                valueTypeAdapter.write(out, value, ctx);
                endObject(value);
            } else {
                valueTypeAdapter.write(out, value, ctx);
            }
        }

        private JsonElement makeReferenceElement(String ref) {
            return new JsonPrimitive(ref);
        }

        public ReferencesPolicy getPolicy() {
            return policy;
        }
    }


    class RefsReadContext implements ReferencesReadContext {

        protected boolean awaitsObjectRead = false;
        protected ReferencePlaceholder<?> lastReadPlaceholder = null;
        protected Map<String, Object> objectsByReference = new HashMap<String, Object>();

        protected void beforeObjectRead(String pathElement) {
            if (awaitsObjectRead) {
                throw new IllegalStateException("Awaits object, but get another path element: " + getCurrentReference());
            }
            if (lastReadPlaceholder != null) {
                throw new IllegalStateException("The last reference placeholder was not consumed, at " +
                        getCurrentReference());
            }
            currentPathElements.addLast(pathElement);
            awaitsObjectRead = true;
        }

        /**
         * Registers an object corresponding to the path built by previous {@link #beforeObjectRead(String)} call.
         *
         * @param value the object created by de-serializers, optionally without fields/contents yet
         * @param fromSimpleTypeAdapter whether the value is read ny a simple type adapter. Such values are
         *                              never referenced by this context
         */
        public void registerObject(Object value, boolean fromSimpleTypeAdapter) {
            if (!awaitsObjectRead) {
                throw new IllegalStateException("registerObject() without corresponding beforeObjectRead(): " +
                        getCurrentReference());
            }
            awaitsObjectRead = false;
            if (value != null && !fromSimpleTypeAdapter) {
                objectsByReference.put(getCurrentReference(), value);
            }
        }

        private Object registerReferenceUse(String reference) {
            Object value = getObjectByReference(reference);
            // the object may now be reference both with the used and the current reference
            registerObject(value, false);
            return value;
        }

        protected void afterObjectRead() {
            if (awaitsObjectRead) {
                throw new IllegalStateException("afterObjectRead() without corresponding registerObject(): " +
                        getCurrentReference());
            }
            currentPathElements.removeLast();
        }


        protected Object getObjectByReference(String reference) throws JsonSyntaxException {
            Object value = objectsByReference.get(reference);
            if (value == null) {
                throw new JsonSyntaxException("Missing reference '" + reference + "'");
            }
            return value;
        }

        public <T> T doRead(JsonReader reader, TypeAdapter<T> typeAdapter, String pathElement,
                            ReadContext ctx) throws IOException {
            beforeObjectRead(pathElement);
            T fieldValue = typeAdapter.read(reader, ctx); // expected registerObject() for non-null reads
            if (fieldValue == null) {
                // registerObject is skipped for nulls in most cases, so clear 'awaits' flag
                awaitsObjectRead = false;
            }
            afterObjectRead();
            return fieldValue;
        }

        public boolean isReferenceString(String str) {
            return str.startsWith(REF_ROOT) && objectsByReference.containsKey(str);
        }

        @SuppressWarnings("unchecked")
        public <T> T getReferencedObject(String reference) throws IOException {
            try {
                Object value = registerReferenceUse(reference);
                if (value instanceof ReferencePlaceholder) {
                    lastReadPlaceholder = (ReferencePlaceholder<?>) value;
                    value = null;
                }

                return (T) value;
            } catch (ClassCastException e) {
                throw new JsonSyntaxException("Incompatible reference type used: " + reference, e);
            }
        }

        @SuppressWarnings("unchecked")
        public <T> ReferencePlaceholder<T> consumeLastPlaceholderIfAny() {
            ReferencePlaceholder<T> result = (ReferencePlaceholder<T>) lastReadPlaceholder;
            lastReadPlaceholder = null;
            return result;
        }

        public ReferencesPolicy getPolicy() {
            return policy;
        }
    }
}
