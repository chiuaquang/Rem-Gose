package org.renpy.android;

import androidx.core.content.FileProvider;

public class RenPyFileProvider extends FileProvider {
    public RenPyFileProvider() {
        // Rem Gose: R class cua app la com.remgose.rem.R (khong phai org.renpy.android.R)
        super(com.remgose.rem.R.xml.file_paths);
    }
}
