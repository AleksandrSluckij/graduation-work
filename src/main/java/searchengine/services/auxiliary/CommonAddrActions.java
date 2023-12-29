package searchengine.services.auxiliary;

import java.net.URI;

public class CommonAddrActions {
  public static URI getUri (String reference) {
    URI uri = null;

    try {
      uri = new URI(reference);
      if (uri.getScheme() == null) {
        uri = null;
      }
    } catch (Exception ignored) {
    }
    return uri;
  }

  public static String extractSiteHost(String address) {
    URI uri = getUri(address);
    return uri == null ? null : uri.getScheme() + "://" + uri.getHost();
  }

  public static String extractPagePath(String fullAddr) {
    URI uri = getUri(fullAddr);
    if (uri == null) {
      return null;
    } else {
      String result = uri.getPath();
      if (result.isEmpty()) {
        result = "/";
      }
      return result;
    }
  }

  public static boolean isGoodAddr (String addrString, String root) {
    boolean result = true;
    result = result && (!addrString.isEmpty());
    result = result && (addrString.startsWith("/") || addrString.startsWith(root));     // this is a subdomain of same domain
    result = result && (!addrString.contains("#"));                                     // this is an anchor
    result = result && (!((addrString.lastIndexOf(".") > addrString.lastIndexOf("/"))
            && !(addrString.endsWith("php") || addrString.endsWith("html") || addrString.endsWith("aspx"))));          // this is a file, but not page
    return result;
  }

  public static String addrNormalization (String addrString, String root) {
    String result;
    if (addrString.startsWith(root)) {
      result = addrString.substring(root.length());
    } else {
      result = addrString;
    }
    if (!result.endsWith("/")) {
      result = result.concat("/");
    }
    return result;
  }

}
