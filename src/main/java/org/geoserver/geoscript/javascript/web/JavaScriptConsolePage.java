package org.geoserver.geoscript.javascript.web;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.geoscript.javascript.JavaScriptModules;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.web.GeoServerSecuredPage;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.WrappedException;
import org.mozilla.javascript.tools.shell.Global;

public class JavaScriptConsolePage extends GeoServerSecuredPage {
    private class Result implements java.io.Serializable {
        private static final long serialVersionUID = 1L;
        @SuppressWarnings("unused")
        public String input, response;
    }

    private List<Result> results = new ArrayList<Result>();
    private String prompt = "";
    private Global global;
    private JavaScriptModules jsModules;

    public JavaScriptConsolePage() {
        jsModules = GeoServerExtensions.bean(JavaScriptModules.class);
        global = jsModules.createGlobal();

        String locator = "geoserver/catalog";
        Object exports = jsModules.require(locator, global);
        if (!(exports instanceof Scriptable)) {
            throw new RuntimeException(
                    "Failed to locate exports in module: " + locator);
        }
        global.put("catalog", global, exports);

        final WebMarkupContainer container = new WebMarkupContainer("results-wrapper");
        final ListView<?> resultsDisplay = 
            new ListView<Object>("results", new PropertyModel(this, "results")) {
                protected void populateItem(ListItem item) {
                    item.add(new Label("javascript", new PropertyModel(item.getModel(), "input")));
                    item.add(new Label("result", new PropertyModel(item.getModel(), "response")));
                }
            };
        container.setOutputMarkupId(true);
        add(container);
        container.add(resultsDisplay);
        Form<JavaScriptConsolePage> f = new Form<JavaScriptConsolePage>("prompt-wrapper", new Model<JavaScriptConsolePage>(this));
        f.setOutputMarkupId(true);
        add(f);
        f.add(new TextField<Object>("prompt", new PropertyModel<Object>(this, "prompt")));
        f.add(new AjaxButton("run", f) {
            /** serialVersionUID */
            private static final long serialVersionUID = 1L;

            protected void onSubmit(AjaxRequestTarget target, Form<?> f) {
                results.add(eval(prompt));
                target.addComponent(container);
            }
        });
    }

    private Result eval(String js) {
        Result res = new Result();
        res.input = js + "\n";

        Context cx = jsModules.enterContext();
        try {
            Object obj = cx.evaluateString(global, js, "<stdin>", 1, null);
            if (obj != Context.getUndefinedValue()) {
                res.response = Context.toString(obj) + "\n";
            }
        } catch (WrappedException we) {
            // Some form of exception was caught by JavaScript and
            // propagated up.
            res.response = we.getWrappedException().toString() + "\n";
            // we.printStackTrace();
        } catch (Exception e) {
            // Some form of JavaScript error.
            res.response = "js: " + e.getMessage() + "\n";
        } finally {
            Context.exit();
        }

        return res;
    }
}
