package Log_Utilities;

import android.util.Log;

public class Array_Printing {
	
	// debugging....
	public static void log_int_array(int[] array, String tag, String arrayname){
		String LPCs_str = "";
		for(int j = 0; j < array.length; j++){
			LPCs_str = LPCs_str + " " + Integer.toString(array[j]);
		}
		Log.i(tag, arrayname + " " + LPCs_str);
	}
	
	public static void log_float_array(float[] array, String tag, String arrayname){
		String LPCs_str = "";
		for(int j = 0; j < array.length; j++){
			LPCs_str = LPCs_str + " " + Float.toString(array[j]);
		}
		Log.i(tag, arrayname + " " + LPCs_str);
	}
	
	public static void log_float_2d_array(float[][] array, String tag, String arrayname){
		String LPCs_str = "";
		for(int r = 0; r < array.length; r++){
			for(int c = 0; c < array[0].length; c++){
				if(c==0){
					LPCs_str = LPCs_str+"\r\n";
				}
				LPCs_str = LPCs_str + " " + Float.toString(array[r][c]);
			}
		}
		Log.i(tag, arrayname + " " + LPCs_str);
	}
	
	

}
