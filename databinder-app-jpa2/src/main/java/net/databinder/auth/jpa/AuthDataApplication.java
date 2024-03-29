/*
 * Databinder: a simple bridge from Wicket to JPA
 * Copyright (C) 2006  Nathan Hamblen nathan@technically.us
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
package net.databinder.auth.jpa;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.servlet.http.HttpServletRequest;

import net.databinder.auth.AuthApplication;
import net.databinder.auth.AuthSession;
import net.databinder.auth.components.jpa.DataSignInPage;
import net.databinder.auth.data.DataUser;
import net.databinder.jpa.DataApplication;
import net.databinder.jpa.Databinder;

import org.apache.wicket.Component;
import org.apache.wicket.Request;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.Response;
import org.apache.wicket.RestartResponseAtInterceptPageException;
import org.apache.wicket.Session;
import org.apache.wicket.authorization.IUnauthorizedComponentInstantiationListener;
import org.apache.wicket.authorization.UnauthorizedInstantiationException;
import org.apache.wicket.authorization.strategies.role.IRoleCheckingStrategy;
import org.apache.wicket.authorization.strategies.role.RoleAuthorizationStrategy;
import org.apache.wicket.authorization.strategies.role.Roles;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.protocol.http.WebRequest;
import org.apache.wicket.util.crypt.Base64UrlSafe;

/**
 * Adds basic authentication functionality to DataApplication. This class is a derivative
 * of Wicket's AuthenticatedWebApplication, brought into the DataApplication hierarchy
 * and including light user specifications in DataUser. You are encouraged to override
 * getUserClass() to implement your own user entity, possibly by extending UserBase.
 * It is also possible to use Databinder authentication without extending this base class
 * by implementing IAuthSettings.
 * <p>Text appearing in authentication components can be overriden for any language, using
 * resource keys listed in their documentation. Except as otherwise noted, these resources
 * can be housed in the application class's properties file, so that subclasses of  the pages
 * and panels are not necessarily required.
 * @see AuthApplication
 * @see DataUser
 * @author Nathan Hamblen
 */
public abstract class AuthDataApplication extends DataApplication
implements IUnauthorizedComponentInstantiationListener, IRoleCheckingStrategy, AuthApplication {

  /**
   * Internal initialization. Client applications should not normally override
   * or call this method.
   */
  @Override
  protected void internalInit() {
    super.internalInit();
    authInit();
  }

  /**
   * Sets Wicket's security strategy for role authorization and appoints this
   * object as the unauthorized instatiation listener. Called automatically on start-up.
   */
  protected void authInit() {
    getSecuritySettings().setAuthorizationStrategy(new RoleAuthorizationStrategy(this));
    getSecuritySettings().setUnauthorizedComponentInstantiationListener(this);
  }

  /**
   * @return new AuthDataSession
   * @see AuthDataSession
   */
  @Override
  public Session newSession(final Request request, final Response response) {
    return new AuthDataSession(request);
  }

  /**
   * Sends to sign in page if not signed in, otherwise throws UnauthorizedInstantiationException.
   */
  public void onUnauthorizedInstantiation(final Component component) {
    if (((AuthSession)Session.get()).isSignedIn()) {
      throw new UnauthorizedInstantiationException(component.getClass());
    }
    else {
      throw new RestartResponseAtInterceptPageException(getSignInPageClass());
    }
  }

  /**
   * Passes query on to the DataUser object if signed in.
   */
  public final boolean hasAnyRole(final Roles roles) {
    final DataUser user = ((AuthSession)Session.get()).getUser();
    if (user != null) {
      for (final String role : roles) {
        if (user.hasRole(role)) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Return user object by matching against a "username" property. Override
   * if you have a differently named property.
   * @return DataUser for the given username.
   */
  public DataUser getUser(final String username) {
    final EntityManager em = Databinder.getEntityManager();

    final CriteriaBuilder qb = em.getCriteriaBuilder();
    final CriteriaQuery<DataUser> cq = qb.createQuery(DataUser.class);
    final Root<DataUser> root = cq.from(DataUser.class);
    cq.select(root).where(qb.equal(root.get("username"), username));
    return em.createQuery(cq).getSingleResult();
  }

  /**
   * Override if you need to customize the sign-in page.
   * @return page to sign in users
   */
  public Class< ? extends WebPage> getSignInPageClass() {
    return DataSignInPage.class;
  }

  /**
   * @return app-salted MessageDigest.
   */
  public MessageDigest getDigest() {
    try {
      final MessageDigest digest = MessageDigest.getInstance("SHA");
      digest.update(getSalt());
      return digest;
    } catch (final NoSuchAlgorithmException e) {
      throw new RuntimeException("SHA Hash algorithm not found.", e);
    }
  }

  /**
   * Get the restricted token for a user, using IP addresses as location parameter. This implementation
   * combines the "X-Forwarded-For" header with the remote address value so that unique
   * values result with and without proxying. (The forwarded header is not trusted on its own
   * because it can be most easily spoofed.)
   * @param user source of token
   * @return restricted token
   */
  public String getToken(final DataUser user) {
    final HttpServletRequest req = ((WebRequest) RequestCycle.get().getRequest()).getHttpServletRequest();
    String fwd = req.getHeader("X-Forwarded-For");
    if (fwd == null) {
      fwd = "nil";
    }
    final MessageDigest digest = getDigest();
    user.getPassword().update(digest);
    digest.update((fwd + "-" + req.getRemoteAddr()).getBytes());
    final byte[] hash = digest.digest(user.getUsername().getBytes());
    return new String(Base64UrlSafe.encodeBase64(hash));
  }
}
