package org.thoughtcrime.securesms.scribbles;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.imageeditor.ColorableRenderer;
import org.thoughtcrime.securesms.imageeditor.ImageEditorMediaConstraints;
import org.thoughtcrime.securesms.imageeditor.ImageEditorView;
import org.thoughtcrime.securesms.imageeditor.Renderer;
import org.thoughtcrime.securesms.imageeditor.model.EditorElement;
import org.thoughtcrime.securesms.imageeditor.model.EditorModel;
import org.thoughtcrime.securesms.imageeditor.renderers.FaceBlurRenderer;
import org.thoughtcrime.securesms.imageeditor.renderers.MultiLineTextRenderer;
import org.thoughtcrime.securesms.mms.MediaConstraints;
import org.thoughtcrime.securesms.providers.PersistentBlobProvider;
import org.thoughtcrime.securesms.scribbles.widget.VerticalSlideColorPicker;
import org.thoughtcrime.securesms.util.MediaUtil;
import org.thoughtcrime.securesms.util.Pair;
import org.thoughtcrime.securesms.util.ParcelUtil;
import org.thoughtcrime.securesms.util.Prefs;
import org.thoughtcrime.securesms.util.Util;

import java.io.ByteArrayOutputStream;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.List;

import static android.app.Activity.RESULT_OK;

public final class ImageEditorFragment extends Fragment implements ImageEditorHud.EventListener,
                                                                   VerticalSlideColorPicker.OnColorChangeListener{

  private static final String TAG = ImageEditorFragment.class.getSimpleName();

  private static final String KEY_IMAGE_URI = "image_uri";

  public static final int SELECT_STICKER_REQUEST_CODE = 123;

  private EditorModel restoredModel;

  private Pair<Uri, FaceDetectionResult> cachedFaceDetection;

  @Nullable
  private EditorElement currentSelection;
  private int           imageMaxHeight;
  private int           imageMaxWidth;

  public static class Data {
    private final Bundle bundle;

    Data(Bundle bundle) {
      this.bundle = bundle;
    }

    public Data() {
      this(new Bundle());
    }

    void writeModel(@NonNull EditorModel model) {
      byte[] bytes = ParcelUtil.serialize(model);
      bundle.putByteArray("MODEL", bytes);
    }

    @Nullable
    public EditorModel readModel() {
      byte[] bytes = bundle.getByteArray("MODEL");
      if (bytes == null) {
        return null;
      }
      return ParcelUtil.deserialize(bytes, EditorModel.CREATOR);
    }
  }

  private Uri             imageUri;
  private ImageEditorHud  imageEditorHud;
  private ImageEditorView imageEditorView;

  public static ImageEditorFragment newInstance(@NonNull Uri imageUri) {
    Bundle args = new Bundle();
    args.putParcelable(KEY_IMAGE_URI, imageUri);

    ImageEditorFragment fragment = new ImageEditorFragment();
    fragment.setArguments(args);
    fragment.setUri(imageUri);
    return fragment;
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    if (imageUri == null) {
      throw new AssertionError("No KEY_IMAGE_URI supplied");
    }

    MediaConstraints mediaConstraints = new ImageEditorMediaConstraints();

    imageMaxWidth  = mediaConstraints.getImageMaxWidth(requireContext());
    imageMaxHeight = mediaConstraints.getImageMaxHeight(requireContext());
  }

  @Nullable
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.image_editor_fragment, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    imageEditorHud  = view.findViewById(R.id.scribble_hud);
    imageEditorView = view.findViewById(R.id.image_editor_view);

    imageEditorHud.setEventListener(this);

    imageEditorView.setTapListener(selectionListener);
    imageEditorView.setDrawingChangedListener(this::refreshUniqueColors);
    imageEditorView.setUndoRedoStackListener(this::onUndoRedoAvailabilityChanged);

    EditorModel editorModel = null;

    if (restoredModel != null) {
      editorModel = restoredModel;
      restoredModel = null;
    } else if (savedInstanceState != null) {
      editorModel = new Data(savedInstanceState).readModel();
    }

    if (editorModel == null) {
      editorModel = new EditorModel();
      EditorElement image = new EditorElement(new UriGlideRenderer(imageUri, true, imageMaxWidth, imageMaxHeight));
      image.getFlags().setSelectable(false).persist();
      editorModel.addElement(image);
    }

    imageEditorView.setModel(editorModel);

    refreshUniqueColors();
  }

  @Override
  public void onSaveInstanceState(@NonNull Bundle outState) {
    super.onSaveInstanceState(outState);
    new Data(outState).writeModel(imageEditorView.getModel());
  }

  public void setUri(@NonNull Uri uri) {
    this.imageUri = uri;
  }

  @NonNull
  public Uri getUri() {
    return imageUri;
  }

  private void changeEntityColor(int selectedColor) {
    if (currentSelection != null) {
      Renderer renderer = currentSelection.getRenderer();
      if (renderer instanceof ColorableRenderer) {
        ((ColorableRenderer) renderer).setColor(selectedColor);
        refreshUniqueColors();
      }
    }
  }

  private void startTextEntityEditing(@NonNull EditorElement textElement, boolean selectAll) {
    imageEditorView.startTextEditing(textElement, Prefs.isIncognitoKeyboardEnabled(getContext()), selectAll);
  }

  protected void addText() {
    String                initialText = "";
    int                   color       = imageEditorHud.getActiveColor();
    MultiLineTextRenderer renderer    = new MultiLineTextRenderer(initialText, color);
    EditorElement         element     = new EditorElement(renderer);

    imageEditorView.getModel().addElementCentered(element, 1);
    imageEditorView.invalidate();

    currentSelection = element;

    startTextEntityEditing(element, true);
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (resultCode == RESULT_OK && requestCode == SELECT_STICKER_REQUEST_CODE && data != null) {
      final String stickerFile = data.getStringExtra(StickerSelectActivity.EXTRA_STICKER_FILE);

      UriGlideRenderer renderer = new UriGlideRenderer(Uri.parse("file:///android_asset/" + stickerFile), false, imageMaxWidth, imageMaxHeight);
      EditorElement element     = new EditorElement(renderer);
      imageEditorView.getModel().addElementCentered(element, 0.2f);
      currentSelection = element;
    } else {
      imageEditorHud.enterMode(ImageEditorHud.Mode.NONE);
    }
  }

  @Override
  public void onModeStarted(@NonNull ImageEditorHud.Mode mode) {
    imageEditorView.setMode(ImageEditorView.Mode.MoveAndResize);
    imageEditorView.doneTextEditing();

    switch (mode) {
      case CROP:
        imageEditorView.getModel().startCrop();
      break;

      case DRAW:
        imageEditorView.startDrawing(0.01f, Paint.Cap.ROUND, false);
        break;

      case HIGHLIGHT:
        imageEditorView.startDrawing(0.03f, Paint.Cap.SQUARE, false);
        break;

      case TEXT:
        addText();
        break;

      case MOVE_DELETE:
        Intent intent = new Intent(getContext(), StickerSelectActivity.class);
        startActivityForResult(intent, SELECT_STICKER_REQUEST_CODE);
        break;

      case NONE:
        imageEditorView.getModel().doneCrop();
        currentSelection = null;
        break;
    }
  }

  @Override
  public void onSave() {
    Util.runOnBackground(() -> {
      Activity activity = ImageEditorFragment.this.getActivity();
      if (activity == null) {
        return;
      }
      Bitmap bitmap = imageEditorView.getModel().render(activity);
      PersistentBlobProvider provider = PersistentBlobProvider.getInstance(ImageEditorFragment.this.getContext());
      ByteArrayOutputStream baos     = new ByteArrayOutputStream();
      bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);

      byte[] data = baos.toByteArray();
      baos   = null;
      bitmap = null;

      Uri    uri    = provider.create(activity, data, MediaUtil.IMAGE_JPEG, null);
      Intent intent = new Intent();
      intent.setData(uri);
      activity.setResult(RESULT_OK, intent);
      activity.finish();
    });
  }

  @Override
  public void onColorChange(int color) {
    imageEditorView.setDrawingBrushColor(color);
    changeEntityColor(color);
  }

  static class BlurTask extends AsyncTask<EditorModel, Void, FaceDetectionResult> {

    interface BlurTaskCallback {
       void onFaceDetectionFinished(FaceDetectionResult result);
    }

    WeakReference<Context> contextRef;
    WeakReference<BlurTaskCallback> callbackRef;

    public BlurTask(Context context, BlurTaskCallback callback) {
      contextRef = new WeakReference<>(context);
      callbackRef = new WeakReference<>(callback);
    }

    @Override
    protected FaceDetectionResult doInBackground(EditorModel... editorModels) {
      EditorModel model = editorModels[0];
      EditorElement element = model.getMainImage();
      Matrix inverseCropPosition = model.getInverseCropPosition();

      Context context = contextRef.get();
      if (context == null) {
        return null;
      }

      if (element.getRenderer() != null) {
        Bitmap bitmap = ((UriGlideRenderer) element.getRenderer()).getBitmap();
        if (bitmap != null) {
          FaceDetector detector = new FirebaseFaceDetector();

          Point size = model.getOutputSizeMaxWidth(1000);
          Bitmap render = model.render(context.getApplicationContext(), size);
          try {
            return new FaceDetectionResult(detector.detect(render), new Point(render.getWidth(), render.getHeight()), inverseCropPosition);
          } finally {
            render.recycle();
            element.getFlags().reset();
          }
        }
      }

      return new FaceDetectionResult(Collections.emptyList(), new Point(0, 0), new Matrix());
    }

    @Override
    protected void onPostExecute(FaceDetectionResult faceDetectionResult) {
      super.onPostExecute(faceDetectionResult);
      BlurTaskCallback callback = callbackRef.get();
      if (callback != null) {
        callback.onFaceDetectionFinished(faceDetectionResult);
      }
    }
  }

  @Override
  public void onBlurFacesToggled(boolean enabled) {
    EditorModel   model     = imageEditorView.getModel();
    EditorElement mainImage = model.getMainImage();
    if (mainImage == null) {
      imageEditorHud.hideBlurToast();
      return;
    }

    if (!enabled) {
      model.clearFaceRenderers();
      imageEditorHud.hideBlurToast();
      return;
    }

    Matrix inverseCropPosition = model.getInverseCropPosition();

    if (cachedFaceDetection != null) {
      if (cachedFaceDetection.first().equals(getUri()) && cachedFaceDetection.second().position.equals(inverseCropPosition)) {
        renderFaceBlurs(cachedFaceDetection.second());
        imageEditorHud.showBlurToast();
        return;
      } else {
        cachedFaceDetection = null;
      }
    }

    //TODO: implement alert
    /*AlertDialog dialog = new AlertDialog.Builder(getContext())
            .setMessage("Blurring... Please wait")
            .setCancelable(false)
            .create();
    dialog.show();*/

    mainImage.getFlags().setChildrenVisible(false);

    BlurTask blurTask = new BlurTask(getContext(), this::onFaceDetectionFinieshed);
    blurTask.doInBackground(model);

    /*SimpleTask.run(getLifecycle(), () -> {
      if (mainImage.getRenderer() != null) {
        Bitmap bitmap = ((UriGlideRenderer) mainImage.getRenderer()).getBitmap();
        if (bitmap != null) {
          FaceDetector detector = new FirebaseFaceDetector();

          Point size = model.getOutputSizeMaxWidth(1000);
          Bitmap render = model.render(ApplicationDependencies.getApplication(), size);
          try {
            return new FaceDetectionResult(detector.detect(render), new Point(render.getWidth(), render.getHeight()), inverseCropPosition);
          } finally {
            render.recycle();
            mainImage.getFlags().reset();
          }
        }
      }

      return new FaceDetectionResult(Collections.emptyList(), new Point(0, 0), new Matrix());
    }, result -> {
      mainImage.getFlags().reset();
      renderFaceBlurs(result);
      progress.dismiss();
      imageEditorHud.showBlurToast();
    });*/
  }

  void onFaceDetectionFinieshed(FaceDetectionResult result) {
    EditorModel   model     = imageEditorView.getModel();
    EditorElement mainImage = model.getMainImage();
    mainImage.getFlags().reset();
    renderFaceBlurs(result);
    imageEditorHud.showBlurToast();
  }

  @Override
  public void onUndo() {
    imageEditorView.getModel().undo();
    refreshUniqueColors();
    imageEditorHud.setBlurFacesToggleEnabled(imageEditorView.getModel().hasFaceRenderer());
  }

  @Override
  public void onDelete() {
    imageEditorView.deleteElement(currentSelection);
    refreshUniqueColors();
  }

  @Override
  public void onFlipHorizontal() {
    imageEditorView.getModel().flipHorizontal();
  }

  @Override
  public void onRotate90AntiClockwise() {
    imageEditorView.getModel().rotate90anticlockwise();
  }

  @Override
  public void onRequestFullScreen(boolean fullScreen, boolean hideKeyboard) {

  }

  @Override
  public void onDone() {

  }

  private void refreshUniqueColors() {
    imageEditorHud.setColorPalette(imageEditorView.getModel().getUniqueColorsIgnoringAlpha());
  }

  private void onUndoRedoAvailabilityChanged(boolean undoAvailable, boolean redoAvailable) {
    imageEditorHud.setUndoAvailability(undoAvailable);
  }

  private void renderFaceBlurs(@NonNull FaceDetectionResult result) {
    List<RectF> faces = result.rects;
    Point       size  = result.imageSize;

    if (faces.isEmpty()) {
      cachedFaceDetection = null;
      return;
    }

    imageEditorView.getModel().pushUndoPoint();

    for (RectF face : faces) {
      FaceBlurRenderer faceBlurRenderer = new FaceBlurRenderer(face, size);
      EditorElement element = new EditorElement(faceBlurRenderer, EditorModel.Z_MASK);
      element.getLocalMatrix().set(result.position);
      imageEditorView.getModel().addElementWithoutPushUndo(element);
    }

    imageEditorView.invalidate();

    cachedFaceDetection = new Pair<>(getUri(), result);
  }

   private final ImageEditorView.TapListener selectionListener = new ImageEditorView.TapListener() {

     @Override
     public void onEntityDown(@Nullable EditorElement editorElement) {
       if (editorElement == null) {
         currentSelection = null;
         imageEditorHud.enterMode(ImageEditorHud.Mode.NONE);
         imageEditorView.doneTextEditing();
       }
     }

     @Override
     public void onEntitySingleTap(@Nullable EditorElement editorElement) {
       currentSelection = editorElement;
       if (currentSelection != null) {
         if (editorElement.getRenderer() instanceof MultiLineTextRenderer) {
           setTextElement(editorElement, (ColorableRenderer) editorElement.getRenderer(), imageEditorView.isTextEditing());
         } else {
           imageEditorHud.enterMode(ImageEditorHud.Mode.MOVE_DELETE);
         }
       }
     }

     @Override
      public void onEntityDoubleTap(@NonNull EditorElement editorElement) {
        currentSelection = editorElement;
        if (editorElement.getRenderer() instanceof MultiLineTextRenderer) {
          setTextElement(editorElement, (ColorableRenderer) editorElement.getRenderer(), true);
        }
      }

     private void setTextElement(@NonNull EditorElement editorElement,
                                 @NonNull ColorableRenderer colorableRenderer,
                                 boolean startEditing)
     {
       int color = colorableRenderer.getColor();
       imageEditorHud.enterMode(ImageEditorHud.Mode.TEXT);
       imageEditorHud.setActiveColor(color);
       if (startEditing) {
         startTextEntityEditing(editorElement, false);
       }
     }
   };

  protected static class FaceDetectionResult {
    private final List<RectF> rects;
    private final Point       imageSize;
    private final Matrix      position;

    FaceDetectionResult(@NonNull List<RectF> rects, @NonNull Point imageSize, @NonNull Matrix position) {
      this.rects     = rects;
      this.imageSize = imageSize;
      this.position  = new Matrix(position);
    }
  }
}
