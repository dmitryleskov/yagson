package am.yagson.refs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A placeholder to a referenced object used when the final object is not known at the moment of
 * the reference registration.
 *
 * @param <T> the expected type of the actual object, to which the placeholder if finally replaced
 */
public class ReferencePlaceholder<T> {

    private T actualObject;

    private List<PlaceholderUse<? extends T>> registeredUses;

    public ReferencePlaceholder() {
    }

    public void registerUse(PlaceholderUse<? extends T> pu) {
        if (registeredUses == null) {
            registeredUses = new ArrayList<PlaceholderUse<? extends T>>();
        }
        registeredUses.add(pu);
    }

    public T getActualObject() {
        return actualObject;
    }

    @SuppressWarnings("unchecked")
    public void applyActualObject(T actualObject) throws IOException {
        this.actualObject = actualObject;
        if (registeredUses != null) {
            for (PlaceholderUse pu : registeredUses) {
                pu.applyActualObject(actualObject);
            }
        }
    }

    @Override
    public String toString() {
        return "ReferencePlaceholder{}";
    }
}