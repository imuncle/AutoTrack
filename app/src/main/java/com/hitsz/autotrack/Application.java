package com.hitsz.autotrack;

public class Application extends android.app.Application {
	static FaceDB mFaceDB;

	@Override
	public void onCreate() {
		super.onCreate();
		mFaceDB = new FaceDB(this.getExternalCacheDir().getPath());
	}
}
