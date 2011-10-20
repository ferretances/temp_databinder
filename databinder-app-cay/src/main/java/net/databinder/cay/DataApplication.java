package net.databinder.cay;

import net.databinder.DataApplicationBase;

import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.cycle.RequestCycleContext;

/**
 * Application base for Cayenne.
 */
public abstract class DataApplication extends DataApplicationBase {

  /** Does nothing, no init required. */
  @Override
  protected void dataInit() {
  }

  /** Returns DataRequestCycle instance for Cayenne. */
  @Override
  public RequestCycle newRequestCycle(
      final RequestCycleContext requestCycleContext) {
    return new DataRequestCycle(requestCycleContext);
  }
}
