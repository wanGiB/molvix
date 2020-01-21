package com.molvix.android.contracts;

/**
 * @author Wan Clem
 */

public interface DoneCallback<T> {

    void done(T result, Exception e);

}
