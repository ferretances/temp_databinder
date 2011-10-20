/*
 * Databinder: a simple bridge from Wicket to Hibernate Copyright (C) 2006
 * Nathan Hamblen nathan@technically.us This library is free software; you can
 * redistribute it and/or modify it under the terms of the GNU Lesser General
 * Public License as published by the Free Software Foundation; either version
 * 2.1 of the License, or (at your option) any later version. This library is
 * distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details. You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */
package net.databinder.auth.components;

import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.interfaces.RSAPublicKey;

import javax.crypto.Cipher;

import net.databinder.auth.valid.EqualPasswordConvertedInputValidator;

import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.util.convert.ConversionException;
import org.apache.wicket.util.crypt.Base64;

/**
 * Note: if equal password validation is need, use
 * EqualPasswordConvertedInputValidator. Equal password inputs are not equal
 * until converted (decrypted).
 * @see EqualPasswordConvertedInputValidator
 */
public class RSAPasswordTextField extends PasswordTextField implements
    IHeaderContributor {

  private static final long serialVersionUID = 1L;

  private static final ResourceReference RSA_JS =
      new JavaScriptResourceReference(RSAPasswordTextField.class, "RSA.js");

  private static final ResourceReference BARRETT_JS =
      new JavaScriptResourceReference(RSAPasswordTextField.class, "Barrett.js");

  private static final ResourceReference BIGINT_JS =
      new JavaScriptResourceReference(RSAPasswordTextField.class, "BigInt.js");

  private String challenge;

  /** 1024 bit RSA key, generated on first access. */
  private static KeyPair keypair;
  static {
    try {
      keypair = KeyPairGenerator.getInstance("RSA").genKeyPair();
    } catch (final NoSuchAlgorithmException e) {
      throw new WicketRuntimeException("Can't find RSA provider", e);
    }
  }

  public RSAPasswordTextField(final String id, final Form form) {
    super(id);
    init(form);
  }

  public RSAPasswordTextField(final String id, final IModel<String> model,
      final Form form) {
    super(id, model);
    init(form);
  }

  @Override
  protected void onRender() {
    getResponse()
        .write(
            "<noscript><div style='color: red;'>Please enable JavaScript and reload this page.</div></noscript>");
    super.onRender();
    getResponse().write(
        "<script>document.getElementById('" + getMarkupId()
            + "').style.visibility='visible';</script>");
  }

  protected void init(final Form form) {
    setOutputMarkupId(true);

    add(new AttributeAppender("style", new Model<String>("visibility:hidden"),
        ";"));

    form.add(new AttributeAppender("onsubmit", new AbstractReadOnlyModel() {
      @Override
      public Object getObject() {
        final StringBuilder eventBuf = new StringBuilder();
        eventBuf.append("if (").append(getElementValue())
            .append(" != null && ").append(getElementValue())
            .append(" != '') ").append(getElementValue())
            .append(" = encryptedString(key, ").append(getChallengeVar())
            .append("+ '|' + ").append(getElementValue()).append(");");

        return eventBuf.toString();
      }
    }, ""));

    challenge =
        new String(Base64.encodeBase64(BigInteger.valueOf(
            new SecureRandom().nextLong()).toByteArray()));
  }

  @Override
  protected String convertValue(final String[] value)
      throws ConversionException {
    final String enc = super.convertValue(value);
    if (enc == null) {
      return null;
    }
    try {
      final Cipher rsa = Cipher.getInstance("RSA");
      rsa.init(Cipher.DECRYPT_MODE, keypair.getPrivate());
      final String dec = new String(rsa.doFinal(hex2data(enc)));

      final String[] toks = dec.split("\\|", 2);
      if (toks.length != 2 || !toks[0].equals(challenge)) {
        throw new ConversionException("incorrect or empy challenge value")
            .setResourceKey("RSAPasswordTextField.failed.challenge");
      }

      return toks[1];
    } catch (final GeneralSecurityException e) {
      throw new ConversionException(e)
          .setResourceKey("RSAPasswordTextField.failed.challenge");
    }
  }

  @Override
  public void renderHead(final IHeaderResponse response) {
    response.renderJavaScriptReference(BIGINT_JS);
    response.renderJavaScriptReference(BARRETT_JS);
    response.renderJavaScriptReference(RSA_JS);

    final RSAPublicKey pub = (RSAPublicKey) keypair.getPublic();
    final StringBuilder keyBuf = new StringBuilder();

    // the key is unique per app instance, send once
    keyBuf.append("setMaxDigits(131);\nvar key= new RSAKeyPair('")
        .append(pub.getPublicExponent().toString(16)).append("', '', '")
        .append(pub.getModulus().toString(16)).append("');");
    response.renderJavaScript(keyBuf.toString(), "rsa_key");

    // the challenge is unique per component instance, send for every component
    final StringBuilder chalBuf = new StringBuilder();
    chalBuf.append("var ").append(getChallengeVar()).append(" = '")
        .append(challenge).append("';");
    response.renderJavaScript(chalBuf.toString(), null);
  }

  protected String getChallengeVar() {
    return getMarkupId() + "_challenge";
  }

  protected String getElementValue() {
    return "document.getElementById('" + getMarkupId() + "').value ";
  }

  // these two functions LGPL, origin:
  // C-JDBC: Clustered JDBC.
  // Copyright (C) 2002-2004 French National Institute For Research In Computer
  // Science And Control (INRIA).
  // Contact: c-jdbc@objectweb.org
  // could be replaced by org.apache.commons.codec.binary.Hex
  private static final byte[] hex2data(final String str) {
    if (str == null) {
      return new byte[0];
    }

    final int len = str.length();
    final char[] hex = str.toCharArray();
    final byte[] buf = new byte[len / 2];

    for (int pos = 0; pos < len / 2; pos++) {
      buf[pos] =
          (byte) (toDataNibble(hex[2 * pos]) << 4 & 0xF0 | toDataNibble(hex[2 * pos + 1]) & 0x0F);
    }

    return buf;
  }

  private static byte toDataNibble(final char c) {
    if ('0' <= c && c <= '9') {
      return (byte) ((byte) c - (byte) '0');
    } else if ('a' <= c && c <= 'f') {
      return (byte) ((byte) c - (byte) 'a' + 10);
    } else if ('A' <= c && c <= 'F') {
      return (byte) ((byte) c - (byte) 'A' + 10);
    } else {
      return -1;
    }
  }
}
