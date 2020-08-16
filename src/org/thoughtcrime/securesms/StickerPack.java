package org.thoughtcrime.securesms;

import android.content.Context;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.versionedparcelable.ParcelField;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StickerPack implements Parcelable {

  public StickerPack(File folder,List<Uri> listOfFiles){
    this.folder=folder;
    this.files=listOfFiles;
  }

  public StickerPack(Parcel parcel, Context context){
    String s = parcel.readString();
    this.folder= new File(s);
    ArrayList<String> uris=new ArrayList<>();
    parcel.readStringList(uris);
    this.files=new ArrayList<>();
    for (String i:uris){
      files.add(Uri.parse(i));
    }
  }

  File folder;
  List<Uri> files;

  protected StickerPack(Parcel in) {
    files = in.createTypedArrayList(Uri.CREATOR);
  }

  public static final Creator<StickerPack> CREATOR = new Creator<StickerPack>() {
    @Override
    public StickerPack createFromParcel(Parcel in) {
      return new StickerPack(in);
    }

    @Override
    public StickerPack[] newArray(int size) {
      return new StickerPack[size];
    }
  };

  public Uri getCategoryPreview(){
    if(files.size()!=0){
      return files.get(0);
    }
    return null;
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeString(folder.getPath());
    ArrayList<String> uris=new ArrayList<>();
    for (Uri i:files){
      uris.add(i.toString());
    }
    dest.writeStringList(uris);
  }
}
