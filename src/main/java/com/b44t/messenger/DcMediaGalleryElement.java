package com.b44t.messenger;

/**
 * Contains a list of media entries, their respective positions and ability to move through it.
 */
public class DcMediaGalleryElement {

  final int[] mediaMsgs;
  int position;
  final DcContext context;
  public DcMediaGalleryElement(int[] mediaMsgs, int position, DcContext context, boolean leftIsRecent) {
    this.mediaMsgs = mediaMsgs;
    this.position = position;
    this.context = context;

    // normal state is left is recent. If the ui needs right to be recent we reverse the order here.
    if (!leftIsRecent) {
      for (int ii = 0; ii < mediaMsgs.length / 2; ii++) {
        int tmp = mediaMsgs[ii];
        int opposite = mediaMsgs.length - 1 - ii;
        mediaMsgs[ii] = mediaMsgs[opposite];
        mediaMsgs[opposite] = tmp;
      }
    }
  }

  public int getCount() {
    return mediaMsgs.length;
  }

  public int getPosition() {
    return position;
  }

  public void moveToPosition(int newPosition) {
    if(newPosition < 0 || newPosition >= mediaMsgs.length)
      throw new IllegalArgumentException("can't move outside of known area.");
    position = newPosition;
  }

  public DcMsg getMessage() {
    return context.getMsg(mediaMsgs[position]);
  }
}
