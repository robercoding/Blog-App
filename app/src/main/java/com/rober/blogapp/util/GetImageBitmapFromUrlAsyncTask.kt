package com.rober.blogapp.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.AsyncTask
import java.net.URL

class GetImageBitmapFromUrlAsyncTask : AsyncTask<String, Unit, Bitmap>() {
    var success = false

    var delegate: AsyncResponse? = null

    override fun doInBackground(vararg params: String?): Bitmap {
        val url = URL(params[0])
        val bitmap = BitmapFactory.decodeStream(url.openStream())
        success = true
        return bitmap
    }

    override fun onPostExecute(result: Bitmap?) {
        super.onPostExecute(result)
        if(result != null){
            delegate?.processFinish(result)
        }
    }
}