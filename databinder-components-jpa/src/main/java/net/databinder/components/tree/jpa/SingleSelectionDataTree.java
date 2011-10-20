package net.databinder.components.tree.jpa;

import java.util.Collection;

import javax.swing.tree.DefaultMutableTreeNode;

import net.databinder.components.tree.data.DataTreeObject;
import net.databinder.models.jpa.JPAListModel;
import net.databinder.models.jpa.JPAObjectModel;

import org.apache.wicket.markup.html.tree.ITreeState;

/**
 * A {@link DataTree} in single selection mode (see {@link ITreeState}), with
 * methods to retrieve the selected node or its backing object.
 * @author Thomas Kappler
 * @param <T> see {@link DataTree}
 */
public abstract class SingleSelectionDataTree<T extends DataTreeObject<T>>
extends DataTree<T> {

  private static final long serialVersionUID = 1L;

  public SingleSelectionDataTree(final String id,
      final JPAObjectModel<T> rootModel) {
    super(id, rootModel);
    getTreeState().setAllowSelectMultiple(false);
  }

  public SingleSelectionDataTree(final String id,
      final JPAListModel<T> childrenModel) {
    super(id, childrenModel);
    getTreeState().setAllowSelectMultiple(false);
  }

  /**
   * Depends on the tree disallowing multiple selection, which we configured in
   * the constructor.
   * @return the currently selected tree node if any, else null
   */
  public DefaultMutableTreeNode getSelectedTreeNode() {
    final Collection<Object> selectedNodes = getTreeState().getSelectedNodes();
    if (selectedNodes.isEmpty()) {
      return null;
    }
    final DefaultMutableTreeNode selected =
      (DefaultMutableTreeNode) selectedNodes.iterator().next();
    return selected;
  }

  /**
   * Return the currently selected user object (of type T).
   * @return the one currently selected T if any, else null
   */
  public T getSelectedUserObject() {
    final DefaultMutableTreeNode selectedNode = getSelectedTreeNode();
    if (selectedNode == null) {
      return null;
    }
    return getDataTreeNode(selectedNode);
  }
}
