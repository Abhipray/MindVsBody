package Log_Utilities;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import android.os.Environment;
import android.util.Log;

public class Utilities {

	
	public static void append_text_to_file(String fname, String txt){
		if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
			String baseDir = Environment.getExternalStorageDirectory().getAbsolutePath();
			String fileName = fname;
			Log.i("FEX",baseDir + File.separator + fileName);
	
			// Not sure if the / is on the path or not
			File f = new File(baseDir + File.separator + fileName);
		    try {
				FileOutputStream fOut = new FileOutputStream(f,true);
				OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
		        myOutWriter.append(txt);
		        myOutWriter.flush();
		        fOut.flush();
		        myOutWriter.close();
		        fOut.close();
		        Log.i("FEX","Succesfully appended to file");
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}	
	}
	
	
}
