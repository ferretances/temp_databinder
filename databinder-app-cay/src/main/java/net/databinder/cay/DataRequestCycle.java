package net.databinder.cay;

import net.databinder.CookieRequestCycle;

import org.apache.cayenne.access.DataContext;
import org.apache.wicket.request.cycle.RequestCycleContext;

/**
 * Request cycle that binds Cayenne context to current thread. Context is
 * rolled back at the end of the request if it contains uncomitted changes.
 */
public class DataRequestCycle extends CookieRequestCycle implements CayenneRequestCycle {

  private DataContext context;

	public DataRequestCycle(final RequestCycleContext requestCycleContext) {
		super(requestCycleContext);
	}

	/** Binds context to this thread. */
	public void contextRequested() {
		if (context == null) {
			context = DataContext.createDataContext();
			DataContext.bindThreadDataContext(context);
		}
	}
	/** Unbinds, rolling back uncomitted changes if any. */
	@Override
	protected void onEndRequest() {
		if (context != null) {
			if (context.hasChanges()) {
        context.rollbackChanges();
      }
			context = null;
			DataContext.bindThreadDataContext(null);
		}
	}
}
