package tz.mengidata.app;

import android.view.View;

/** A screen hosted by {@link MainActivity}. */
interface Page {
    View root();

    void refresh();

    String title();

    String subtitle();

    /** Called by the green "+" button in the top bar. */
    void onAdd();

    /** Returns true if the page consumed the back press. */
    boolean onBack();
}
