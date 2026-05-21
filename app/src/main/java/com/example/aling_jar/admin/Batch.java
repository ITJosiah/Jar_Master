package com.example.aling_jar.admin;

/**
 * Backwards-compatibility alias.
 * <p>
 * The canonical {@link com.example.aling_jar.data.model.Batch} now lives in the
 * shared data layer. This subclass keeps every existing admin file compiling
 * without import changes while we progressively migrate.
 *
 * @deprecated Use {@link com.example.aling_jar.data.model.Batch} directly.
 */
@Deprecated
public class Batch extends com.example.aling_jar.data.model.Batch {

    public Batch() {
        super();
    }
}
