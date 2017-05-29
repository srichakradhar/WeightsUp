package chuckree.weightsup.data;

import android.provider.BaseColumns;

/**
 * Created by chuck on 11/05/17.
 */

public class WeightLogContract {

    public static final class WeightLogEntry implements BaseColumns {

        public static final String TABLE_NAME = "weightlog";
        public static final String COLUMN_WEIGHT = "weight";
        public static final String COLUMN_IMAGE_PATH = "image_path";
        public static final String COLUMN_TIMESTAMP = "timestamp";

    }

}
