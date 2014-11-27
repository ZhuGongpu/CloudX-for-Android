package cloudx.model;

import cloudx.main.R;

/**
 * Created by zhugongpu on 14/11/20.
 */
public class FileEntity {
    public String fileName = null;
    public String filePath = null;
    public String fileSize;

    public FileType fileType;

   public int getFileIcon() {
        switch (fileType) {

            case Doc:


            case PPT:


            case Image:


            case Others:
                return R.drawable.file;
        }
        return R.drawable.file;
    }

    public enum FileType {
        Doc, PPT, Image, Others
    }
}
