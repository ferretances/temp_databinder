package net.databinder.models;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;

/**
 * Projects a single list into multiple, arbitrarily transformed sublists
 * without replicating the list structure. A parent list containing sublists is
 * the object wrapped in this model, while the master list model passed in is
 * held and used internally.
 * <p>
 * If you need to detach the master list, detach this model and the command will
 * be passed to the contained master list. Any action that changes the size of
 * the master list must also detach this model so it can recalculate the sublist
 * count.
 * @author Nathan Hamblen
 */
public abstract class SublistProjectionModel<T> extends
LoadableDetachableModel<T> {

  private static final long serialVersionUID = 1L;
  /** Continuous list used to feed this model's sublists. */
  private final IModel<T> master;

  public SublistProjectionModel(final IModel<T> master) {
    this.master = master;
  }

  /** @return number of sublists */
  protected abstract int getParentSize();

  /** @return index of master list mapped to parameters */
  protected abstract int transform(int parentIdx, int sublistIdx);

  /** @return size sublist at given index */
  protected abstract int getSize(int parentIdx);

  /**
   * Breaks the parent list into chunks of the requested size, in the same order
   * as the parent list.
   */
  public static class Chunked<T> extends SublistProjectionModel<T> {

    /** */
    private static final long serialVersionUID = 1L;
    protected int chunkSize;

    public Chunked(final int chunkSize, final IModel<T> master) {
      super(master);
      this.chunkSize = chunkSize;
    }

    @Override
    protected int transform(final int parentIdx, final int sublistIdx) {
      return parentIdx * chunkSize + sublistIdx;
    }

    @Override
    protected int getSize(final int parentIdx) {
      return Math
      .min(getMasterList().size() - parentIdx * chunkSize, chunkSize);
    }

    @Override
    protected int getParentSize() {
      return (getMasterList().size() - 1) / chunkSize + 1;
    }
  }

  /**
   * Transposes rows and columns so the list runs top to bottom rather than left
   * to right.
   */
  public static class Transposed<T> extends Chunked<T> {

    /** */
    private static final long serialVersionUID = 1L;

    public Transposed(final int columns, final IModel<T> master) {
      super(columns, master);
    }

    @Override
    protected int transform(final int parentIdx, final int sublistIdx) {
      return parentIdx + sublistIdx * getParentSize();
    }

    @Override
    protected int getSize(final int parentIdx) {
      return (getMasterList().size() - parentIdx - 1) / getParentSize() + 1;
    }

  }

  @SuppressWarnings("unchecked")
  protected List<T> getMasterList() {
    return (List<T>) master.getObject();
  }

  @Override
  @SuppressWarnings("unchecked")
  protected T load() {
    final int rows = getParentSize();
    final List<List<T>> parent = new ArrayList<List<T>>(rows);
    for (int i = 0; i < rows; i++) {
      parent.add(new ProjectedSublist(i));
    }

    return (T) parent;
  }

  /**
   * This is a virtual list, a projection of the master list. Its size and index
   * trasform is governed by the containing object.
   */
  protected class ProjectedSublist extends AbstractList<T> {
    private final int parentIdx;

    public ProjectedSublist(final int parentIdx) {
      this.parentIdx = parentIdx;
    }

    @Override
    public T get(final int index) {
      return getMasterList().get(transform(parentIdx, index));
    }

    @Override
    public int size() {
      return getSize(parentIdx);
    }
  }

  /**
   * Detach master list.
   */
  @Override
  protected void onDetach() {
    master.detach();
  }
}
