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
import org.mozilla.javascript.tools.shell.Global;

public class JavaScriptConsolePage extends GeoServerSecuredPage {
    private class Result implements java.io.Serializable {
        private static final long serialVersionUID = 1L;
        public String input, response;
        public boolean success = true;
    }

    private List<Result> results = new ArrayList<Result>();
    private String prompt = "";
    private Global global;
    private JavaScriptModules jsModules;

    public JavaScriptConsolePage() {
        this.jsModules = GeoServerExtensions.bean(JavaScriptModules.class);;
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
        res.input = js;

        Context cx = jsModules.enterContext();
        try {
            res.response = (String) Context.jsToJava(cx.evaluateString(global, js, "<input>", 1, null), String.class);
        } catch(Exception e) {
            res.success = false;
            res.response = e.getMessage();
        } finally {
            Context.exit();
        }

        return res;
    }
}
