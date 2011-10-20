package net.databinder.components.tree.jpa;

import javax.persistence.EntityManager;
import javax.swing.tree.DefaultMutableTreeNode;

import net.databinder.components.tree.data.DataTreeObject;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.form.Form;

/**
 * Delete the selected node. Works only with {@link SingleSelectionDataTree} to
 * avoid dealing with multiple selected nodes.
 * <p>
 * The root cannot be deleted, it must be handled elsewhere in the application.
 * This follows the Sun <a
 * href="http://java.sun.com/docs/books/tutorial/uiswing/components/tree.html"
 * >How to Use Trees</a> tutorial, example DynamicTreeDemo.
 * </p>
 * @author Thomas Kappler
 * @param <T> see {@link DataTree}
 */
public class DataTreeDeleteButton<T extends DataTreeObject<T>> extends
AjaxButton {

  private SingleSelectionDataTree<T> tree;
  private boolean deleteOnlyLeafs = true;

  public DataTreeDeleteButton(final String id,
      final SingleSelectionDataTree<T> tree) {
    super(id);
    this.tree = tree;
    setDefaultFormProcessing(false);
  }

  public DataTreeDeleteButton(final String id,
      final SingleSelectionDataTree<T> tree, final boolean deleteOnlyLeafs) {
    this(id, tree);
    this.deleteOnlyLeafs = deleteOnlyLeafs;
  }

  @Override
  public boolean isEnabled() {
    final DefaultMutableTreeNode selected = tree.getSelectedTreeNode();
    if (selected == null) {
      return false;
    }
    if (selected.isRoot()) {
      return false;
    }
    if (deleteOnlyLeafs) {
      return selected.isLeaf();
    }

    return true;
  }

  @Override
  protected void onSubmit(final AjaxRequestTarget target, final Form form) {
    final DefaultMutableTreeNode selectedNode = tree.getSelectedTreeNode();
    final T selected = tree.getSelectedUserObject();

    final DefaultMutableTreeNode parentNode =
      (DefaultMutableTreeNode) selectedNode.getParent();
    final T parent = tree.getDataTreeNode(parentNode);

    if (parent != null) {
      parent.getChildren().remove(selected);
    }
    parentNode.remove(selectedNode);

    final EntityManager em = net.databinder.jpa.Databinder.getEntityManager();
    if (em.contains(selected)) {
      em.remove(selected);
      em.getTransaction().commit();
    }

    tree.getTreeState().selectNode(parentNode, true);
    tree.repaint(target);
    tree.updateDependentComponents(target, parentNode);
  }

  /**
   * @see org.apache.wicket.ajax.markup.html.form.AjaxButton#onError(org.apache.wicket.ajax.AjaxRequestTarget,
   *      org.apache.wicket.markup.html.form.Form)
   */
  @Override
  protected void onError(final AjaxRequestTarget target, final Form<?> form) {
  }
}