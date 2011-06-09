package org.geoserver.geoscript.javascript.web;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
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
import org.geoserver.web.GeoServerSecuredPage;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.tools.shell.Global;

public class RhinoConsolePage extends GeoServerSecuredPage {
    private class Result implements java.io.Serializable {
        private static final long serialVersionUID = 1L;
        public String input, response;
        public boolean success = true;
    }

    private List<Result> results = new ArrayList<Result>();
    private String prompt = "";
    private Global global;

    public RhinoConsolePage() {

        Context cx = Context.enter();
        cx.setLanguageVersion(170);
        global = new Global();
        global.initStandardObjects(cx, true);
        String modulePath;
        try {
            modulePath = JavaScriptModules.getModulePath();
        } catch (URISyntaxException e) {
            throw new RuntimeException("Trouble evaluating module path.", e);
        }
        global.installRequire(
            cx, 
            (List<String>) Arrays.asList(modulePath), 
            false
        );

        Context.exit();

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
        Form<RhinoConsolePage> f = new Form<RhinoConsolePage>("prompt-wrapper", new Model<RhinoConsolePage>(this));
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
        Context cx = Context.enter();
        cx.setLanguageVersion(170);

        Result res = new Result();
        res.input = js;

        try {
            res.response = (String) Context.jsToJava(cx.evaluateString(global, js, "<input>", 1, null), String.class);
        } catch(Exception e) {
            res.success = false;
            res.response = e.getMessage();
        }

        Context.exit();

        return res;
    }

}
