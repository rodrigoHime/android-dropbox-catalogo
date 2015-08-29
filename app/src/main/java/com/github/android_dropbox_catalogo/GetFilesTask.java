
package com.github.android_dropbox_catalogo;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.DropboxAPI.Entry;
import com.dropbox.client2.DropboxAPI.ThumbFormat;
import com.dropbox.client2.DropboxAPI.ThumbSize;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.exception.DropboxIOException;
import com.dropbox.client2.exception.DropboxParseException;
import com.dropbox.client2.exception.DropboxPartialFileException;
import com.dropbox.client2.exception.DropboxServerException;
import com.dropbox.client2.exception.DropboxUnlinkedException;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class GetFilesTask extends AsyncTask<Void, Long, Boolean> {

    private GridViewAdapter mGridVIewAdapter;
    private Context mContext;
    private DropboxAPI<?> mApi;
    private String mPath;
    private ImageView mView;
    private Drawable mDrawable;

    private FileOutputStream mFos;

    private Long mFileLen;
    private String mErrorMsg;
    private static final String IMAGE_CACHE_DIR = "images";

    // Note that, since we use a single file name here for simplicity, you
    // won't be able to use this code for two simultaneous downloads.
    private final static String IMAGE_FILE_NAME = "dbroulette.png";
    private final ArrayList<ImageItem> imageItems = new ArrayList<ImageItem>();
    private ProgressDialog mDialog;

    public GetFilesTask(Context context, DropboxAPI<?> api,
                        String dropboxPath, ImageView view, GridViewAdapter gridViewAdapter) {
        // We set the context this way so we don't accidentally leak activities
        mContext = context;
        mApi = api;
        mPath = dropboxPath;
        mView = view;
        mGridVIewAdapter = gridViewAdapter;
    }

    @Override
    protected void onPreExecute() {
        mDialog = new ProgressDialog(mContext);
        mDialog.setMessage("Downloading...");
        mDialog.setButton(ProgressDialog.BUTTON_POSITIVE, "Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                // mCanceled = true;
                // mErrorMsg = "Canceled";
                // This will cancel the getThumbnail operation by closing
                // its stream
                if (mFos != null) {
                    try {
                        mFos.close();
                    } catch (IOException e) {
                    }
                }
            }
        });
        mDialog.show();
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        try {

            // Get the metadata for a directory
            Entry dirent = mApi.metadata(mPath, 1000, null, true, null);

            // Make a list of everything in it that we can get a thumbnail for
            for (Entry ent: dirent.contents) {
                if (ent.thumbExists) {
                    // Add it to the list of thumbs we can choose from
                    DropboxAPI.DropboxInputStream stream =
                            mApi.getThumbnailStream(ent.path, ThumbSize.BESTFIT_480x320, ThumbFormat.JPEG);
                    Bitmap bitmap = BitmapFactory.decodeStream(stream);
                    imageItems.add(new ImageItem(bitmap, ent.fileName()));
                    try {
                        if (stream != null) {
                            stream.close();
                        }
                    } catch (IOException e) {
                        Log.w("GetFilesTask", "Error while retrieving bitmap from " + ent.fileName());
                    }
                }
            }





            // This downloads a smaller, thumbnail version of the file.  The
            // API to download the actual file is roughly the same.
//            mApi.getThumbnail(path, mFos, ThumbSize.BESTFIT_960x640,
//                    ThumbFormat.JPEG, null);
//            if (mCanceled) {
//                return false;
//            }

//            mDrawable = Drawable.createFromPath(cachePath);
            // We must have a legitimate picture
            return true;

        } catch (DropboxUnlinkedException e) {
            // The AuthSession wasn't properly authenticated or user unlinked.
        } catch (DropboxPartialFileException e) {
            // We canceled the operation
            mErrorMsg = "Download canceled";
        } catch (DropboxServerException e) {
            // Server-side exception.  These are examples of what could happen,
            // but we don't do anything special with them here.
            if (e.error == DropboxServerException._304_NOT_MODIFIED) {
                // won't happen since we don't pass in revision with metadata
            } else if (e.error == DropboxServerException._401_UNAUTHORIZED) {
                // Unauthorized, so we should unlink them.  You may want to
                // automatically log the user out in this case.
            } else if (e.error == DropboxServerException._403_FORBIDDEN) {
                // Not allowed to access this
            } else if (e.error == DropboxServerException._404_NOT_FOUND) {
                // path not found (or if it was the thumbnail, can't be
                // thumbnailed)
            } else if (e.error == DropboxServerException._406_NOT_ACCEPTABLE) {
                // too many entries to return
            } else if (e.error == DropboxServerException._415_UNSUPPORTED_MEDIA) {
                // can't be thumbnailed
            } else if (e.error == DropboxServerException._507_INSUFFICIENT_STORAGE) {
                // user is over quota
            } else {
                // Something else
            }
            // This gets the Dropbox error, translated into the user's language
            mErrorMsg = e.body.userError;
            if (mErrorMsg == null) {
                mErrorMsg = e.body.error;
            }
        } catch (DropboxIOException e) {
            // Happens all the time, probably want to retry automatically.
            mErrorMsg = "Network error.  Try again.";
        } catch (DropboxParseException e) {
            // Probably due to Dropbox server restarting, should retry
            mErrorMsg = "Dropbox error.  Try again.";
        } catch (DropboxException e) {
            // Unknown error
            mErrorMsg = "Unknown error.  Try again.";
        }
        return false;
    }

    @Override
    protected void onPostExecute(Boolean result) {
        mDialog.dismiss();
        if (result) {
            // Set the image now that we have it
            mGridVIewAdapter.setData(imageItems);
        } else {
            // Couldn't download it, so show an error
//             showToast(mErrorMsg);
        }
    }
}
