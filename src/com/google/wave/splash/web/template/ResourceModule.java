package com.google.wave.splash.web.template;

import com.google.inject.Singleton;
import com.google.inject.servlet.ServletModule;

/**
 * Sets up mappings for static resources served from this package.
 *
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public class ResourceModule extends ServletModule {

  @Override
  protected void configureServlets() {
    bind(ResourceServlet.class).in(Singleton.class);

    serve("*.png").with(ResourceServlet.class);
    serve("*.jpg").with(ResourceServlet.class);
    serve("*.gif").with(ResourceServlet.class);

    serve("*.js").with(ResourceServlet.class);
    serve("*.css").with(ResourceServlet.class);
  }
}
