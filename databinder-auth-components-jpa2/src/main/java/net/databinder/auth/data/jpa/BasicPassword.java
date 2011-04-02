package net.databinder.auth.data.jpa;

import java.io.Serializable;
import java.security.MessageDigest;

import javax.persistence.Column;
import javax.persistence.Embeddable;

import net.databinder.auth.AuthApplication;
import net.databinder.auth.data.DataPassword;

import org.apache.wicket.Application;
import org.apache.wicket.util.crypt.Base64;

/**
 * Simple, optional implementation of {@link DataPassword}. Maps as an embedded
 * property to the single field "passwordHash".
 * @author Nathan Hamblen
 */
@Embeddable
public class BasicPassword implements DataPassword, Serializable {

  private static final long serialVersionUID = 1L;

  private String passwordHash;

  public BasicPassword() {
  }

  public BasicPassword(final String password) {
    change(password);
  }

  public void change(final String password) {
    final MessageDigest md = ((AuthApplication) Application.get()).getDigest();
    final byte[] hash = md.digest(password.getBytes());
    passwordHash = new String(Base64.encodeBase64(hash));
  }

  public void update(final MessageDigest md) {
    md.update(passwordHash.getBytes());
  }

  @Column(length = 28, nullable = false)
  private String getPasswordHash() {
    return passwordHash;
  }

  @SuppressWarnings("unused")
  private void setPasswordHash(final String passwordHash) {
    this.passwordHash = passwordHash;
  }

  public boolean matches(final String password) {
    return passwordHash != null
    && passwordHash
    .equals(new BasicPassword(password).getPasswordHash());
  }
}
