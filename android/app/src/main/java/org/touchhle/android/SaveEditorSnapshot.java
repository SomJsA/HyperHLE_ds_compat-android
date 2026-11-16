package org.touchhle.android;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;

/** Read each independent editor section without making an empty story slot disable settings. */
final class SaveEditorSnapshot {
    SaveScalarEditor.Slot slot;
    SaveSettingsEditor.Snapshot settings;
    final LinkedHashMap<String,String> fields = new LinkedHashMap<>();
    String storyError = "", settingsError = "";

    static SaveEditorSnapshot read(File base, int index) throws IOException {
        if (index < 0 || index > 2) throw new IOException("Invalid slot");
        SaveEditorSnapshot result = new SaveEditorSnapshot();
        try {
            result.slot = SaveScalarEditor.read(base, index);
            List<File> files = SaveScalarEditor.slotFiles(base, index);
            result.fields.putAll(SaveScalarEditor.readFields(files.get(0)));
        } catch (IOException error) {
            result.slot = null;
            result.storyError = error.getMessage();
        }
        try { result.settings = SaveSettingsEditor.read(base); }
        catch (IOException error) { result.settingsError = error.getMessage(); }
        return result;
    }
}
