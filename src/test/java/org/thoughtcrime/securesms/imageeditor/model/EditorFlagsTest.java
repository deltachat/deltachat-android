package org.thoughtcrime.securesms.imageeditor.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class EditorFlagsTest {

  @Test
  public void defaults() {
    EditorFlags flags = new EditorFlags();
    assertTrue(flags.isAspectLocked());
    assertTrue(flags.isSelectable());
    assertTrue(flags.isVisible());
    assertTrue(flags.isChildrenVisible());
    assertTrue(flags.isEditable());
    assertFalse(flags.isRotateLocked());
  }

  @Test
  public void setAndClearSingleFlag() {
    EditorFlags flags = new EditorFlags();
    flags.setVisible(false);
    assertFalse(flags.isVisible());
    assertTrue(flags.isSelectable());
    flags.setVisible(true);
    assertTrue(flags.isVisible());
  }

  @Test
  public void persistAndResetRestoresState() {
    EditorFlags flags = new EditorFlags();
    flags.setEditable(false);
    flags.persist();
    assertEquals(flags.asInt(), flags.getCurrentState());
    flags.setVisible(false);
    flags.reset();
    assertFalse(flags.isEditable());
    assertTrue(flags.isVisible());
  }

  @Test
  public void resetWithoutPersistRestoresInitialState() {
    EditorFlags flags = new EditorFlags();
    flags.setVisible(false);
    flags.reset();
    assertTrue(flags.isVisible());
  }

  @Test
  public void restoreMarkedState() {
    EditorFlags flags = new EditorFlags();
    flags.setVisible(false);
    flags.mark();
    flags.setEditable(false);
    flags.restore();
    assertFalse(flags.isVisible());
    assertTrue(flags.isEditable());
  }

  @Test
  public void setCopiesCurrentAndPersistedState() {
    EditorFlags a = new EditorFlags();
    a.setVisible(false);
    a.persist();

    EditorFlags b = new EditorFlags();
    b.set(a);

    assertFalse(b.isVisible());
    b.reset();
    assertFalse(b.isVisible());
  }

  @Test
  public void setPreservesOwnMarkedState() {
    EditorFlags b = new EditorFlags();
    b.setEditable(false);
    b.mark();

    b.set(new EditorFlags());

    b.restore();
    assertFalse(b.isEditable());
    assertTrue(b.isVisible());
  }
}
