package net.databinder.components.tree.jpa;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;

import net.databinder.components.tree.data.DataTreeObject;
import net.databinder.models.jpa.BasicPredicateBuilder;
import net.databinder.models.jpa.JPAListModel;
import net.databinder.models.jpa.JPAObjectModel;
import net.databinder.util.CriteriaDefinition;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.tree.BaseTree;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

/**
 * An extension of {@link BaseTree} based on node objects being represented by
 * {@link JPAObjectModel}s. Additionally, it offers some convenience
 * methods.
 * @author Thomas Kappler
 * @param <T> the IDataTreeNode implementation being represented by the tree
 *          nodes
 */
public abstract class DataTree<T extends DataTreeObject<T>> extends BaseTree {
  /** */
  private static final long serialVersionUID = 1L;

  /**
   * Construct a tree with a root entity.
   * @param id Wicket id
   * @param rootModel must contain a root of type T
   */
  public DataTree(final String id, final JPAObjectModel<T> rootModel) {
    super(id);
    final DefaultMutableTreeNode rootNode =
      new DefaultMutableTreeNode(rootModel);
    populateTree(rootNode, rootModel.getObject().getChildren());
    setDefaultModel(new Model<DefaultTreeModel>(new DefaultTreeModel(rootNode)));
  }

  /**
   * Construct a rootless tree based on a list of top level nodes.
   * @param id
   * @param topLevelModel must contain a List<T> of top level children
   */
  public DataTree(final String id, final JPAListModel<T> topLevelModel) {
    super(id);
    setRootLess(true);
    final DefaultMutableTreeNode rootNode =
      new DefaultMutableTreeNode(topLevelModel);
    populateTree(rootNode, topLevelModel.getObject());
    setDefaultModel(new Model<DefaultTreeModel>(new DefaultTreeModel(rootNode)));
  }

  /**
   * Convenience criteria builder for fetching top-level entities.
   */
  public static class TopLevelCriteriaBuilder<T> extends
  BasicPredicateBuilder<T> {

    public TopLevelCriteriaBuilder(final Class<T> entityClass) {
      super(entityClass);
    }

    private static final long serialVersionUID = 1L;

    /**
     * build criteria for a null "parent" property
     */
    public void build(final CriteriaBuilder predicates) {
    }

    @Override
    public void build(final List<Predicate> predicates) {
      final CriteriaDefinition<T> cd = getCriteriaDefinition();
      final Root<T> root = cd.getRoot();
      final CriteriaBuilder cb = cd.getCriteriaBuilder();
      final Predicate p = cb.isNotNull(root.get("parent"));
      predicates.add(p);
      cd.addAllPredicates(predicates);
    }

  }

  public DefaultMutableTreeNode clear(final AjaxRequestTarget target) {
    final T newObject = createNewObject();
    final DefaultMutableTreeNode newRootNode =
      new DefaultMutableTreeNode(new JPAObjectModel<T>(newObject));
    final TreeModel treeModel = new DefaultTreeModel(newRootNode);
    setDefaultModel(new Model<Serializable>((Serializable) treeModel));
    repaint(target);
    return newRootNode;
  }

  /**
   * Recursively build the tree nodes according to the structure given by the
   * beans.
   * @param parent a tree node serving as parent to the newly created nodes for
   *          the elements in children
   * @param children objects to be inserted into the tree below parent
   */
  private void populateTree(final DefaultMutableTreeNode parent,
      final Collection<T> children) {
    for (final T t : children) {
      final JPAObjectModel<T> m = new JPAObjectModel<T>(t);
      final DefaultMutableTreeNode node = new DefaultMutableTreeNode(m);
      parent.add(node);
      populateTree(node, t.getChildren());
    }
  }

  /**
   * Get the IDataTreeNode instance behind this node, or null if the node is the
   * root of a tree with no root entity.
   * @param node a tree node
   * @return the object represented by node
   */
  @SuppressWarnings("unchecked")
  public T getDataTreeNode(final DefaultMutableTreeNode node) {
    final Object nodeObject = ((IModel<T>) node.getUserObject()).getObject();
    return nodeObject instanceof DataTreeObject<?> ? (T) nodeObject : null;
  }

  /**
   * @return the root node of the tree
   */
  public DefaultMutableTreeNode getRootNode() {
    final DefaultTreeModel treeModel =
      (DefaultTreeModel) getDefaultModelObject();
    if (treeModel.getRoot() == null) {
      return null;
    }
    return (DefaultMutableTreeNode) treeModel.getRoot();
  }

  /**
   * Create a new user object using {@link #createNewObject()} and add it to the
   * tree as a child of parentNode.
   * @param parentNode to node serving as parent of the new object
   * @return the newly created tree node
   */
  public DefaultMutableTreeNode addNewChildNode(
      final DefaultMutableTreeNode parentNode) {
    final T newObject = createNewObject();

    final T parent = getDataTreeNode(parentNode);
    if (parent != null) {
      parent.addChild(newObject);
    }

    final DefaultMutableTreeNode newNode =
      new DefaultMutableTreeNode(new JPAObjectModel<T>(newObject));
    parentNode.add(newNode);
    return newNode;
  }

  /**
   * Repaint the tree when something has changed. It possibly does too much, but
   * you're safe that changes do show after you call it.
   * @param target
   */
  public void repaint(final AjaxRequestTarget target) {
    invalidateAll();
    updateTree(target);
  }

  /**
   * Create a new instance of T. Used to create the backing objects of new tree
   * nodes.
   * @return a new instance of T
   */
  protected abstract T createNewObject();

  /**
   * Override to update components when another tree node is selected. Does
   * nothing by default.
   * @param target
   * @param selectedNode the currently selected node
   */
  public void updateDependentComponents(final AjaxRequestTarget target,
      final DefaultMutableTreeNode selectedNode) {
    // Do nothing by default
  }

  @Override
  public void onDetach() {
    super.onDetach();
    // in a root less tree it's not bound to any component
    ((IModel<T>) getRootNode().getUserObject()).detach();
  }
}
