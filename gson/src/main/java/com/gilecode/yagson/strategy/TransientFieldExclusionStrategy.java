/*
 * Copyright (C) 2016 Andrey Mogilev
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gilecode.yagson.strategy;

import java.lang.reflect.Field;

/**
 * An alternative to {@link com.google.gson.ExclusionStrategy} which is applied only for transient fields allowed by the
 * general exclusion strategies.
 *
 * @author Andrey Mogilev
 */
public interface TransientFieldExclusionStrategy {

    /**
     * @param f the field object that is under test
     * @return true if the field should be ignored; otherwise false
     */
    boolean shouldSkipField(Field f);
}
