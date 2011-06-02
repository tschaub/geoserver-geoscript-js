package org.geoserver.geoscript.javascript.web;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

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

import org.mozilla.javascript.Context;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.WrappedException;

import org.geotools.util.logging.Logging;

import org.geoserver.web.GeoServerSecuredPage;

public class RhinoConsolePage extends GeoServerSecuredPage {
    private class Result implements java.io.Serializable {
        public String input, response;
        public boolean success = true;
    }

    private ScriptableObject scope;
    private List<Result> results = new ArrayList<Result>();
    private String prompt = "helo> ";

    private Result eval(String js) {
        Context cx = Context.enter();

        Result res = new Result();
        res.input = js;

        try {
            res.response = (String) Context.jsToJava(cx.evaluateString(scope, js, "<input>", 1, null), String.class);
        } catch(EvaluatorException e) {
            res.success = false;
            res.response = e.getMessage();
        }

        Context.exit();

        return res;
    }

    private static final Logger LOGGER = Logging.getLogger("org.geoserver.geoscript.javascript.web");
    public RhinoConsolePage() {
        Context cx = Context.enter();
        scope = cx.initStandardObjects();
        Object wrappedCatalog = Context.javaToJS(getCatalog(), scope);
        ScriptableObject.putProperty(scope, "catalog", wrappedCatalog);
        Context.exit();

        final WebMarkupContainer container = new WebMarkupContainer("results-wrapper");
        final ListView resultsDisplay = 
            new ListView("results", new PropertyModel(this, "results")) {
                protected void populateItem(ListItem item) {
                    item.add(new Label("javascript", new PropertyModel(item.getModel(), "input")));
                    item.add(new Label("result", new PropertyModel(item.getModel(), "response")));
                }
            };
        container.setOutputMarkupId(true);
        add(container);
        container.add(resultsDisplay);
        Form f = new Form("prompt-wrapper", new Model(this));
        f.setOutputMarkupId(true);
        add(f);
        f.add(new TextField("prompt", new PropertyModel(this, "prompt")));
        f.add(new AjaxButton("run", f) {
            protected void onSubmit(AjaxRequestTarget target, Form f) {
                results.add(eval(prompt));
                target.addComponent(container);
            }
        });
    }
}
