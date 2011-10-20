package net.databinder.components;

import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RefreshingView;
import org.apache.wicket.request.resource.ResourceReference;

/**
 * Base class for Item classes in Wicket's repeater package.
 */
public abstract class RepeaterItemButton extends BaseItemButton {
   
   Item item;
   
   public RepeaterItemButton(String id, Item item, ResourceReference image) {
      super(id, image);
      this.item = item;
   }
   
   protected RefreshingView getView() {
      return (RefreshingView) item.getParent();
   }
}