package com.vaadin.elements.grid.data;

import java.util.List;

import com.google.gwt.core.client.JavaScriptObject;
import com.vaadin.elements.common.js.Function;
import com.vaadin.elements.common.js.JS;
import com.vaadin.elements.grid.GridElement;
import com.vaadin.elements.grid.config.JSDataRequest;

/**
 * Datasource where requestRows() is delegated to a js native function
 */
public class GridJsFuncDataSource extends GridDataSource {

    private JavaScriptObject jsFunction;
    private boolean initialRowSetReceived;

    public GridJsFuncDataSource(JavaScriptObject jso, GridElement grid) {
        super(grid);
        jsFunction = jso;
        // We need to do a first query to DB in order to get the initial size
        // and then attach the data-source to the grid, otherwise the grid will
        // never call the requestRows method when size is zero.
        requestRows(0, 0, null);
    }

    public void setJSFunction(JavaScriptObject jso) {
        jsFunction = jso;
        clearCache(null);
        gridElement.getSelectionModel().reset();
    }

    @Override
    protected void requestRows(final int firstRowIndex, final int numberOfRows,
            final RequestRowsCallback<Object> callback) {

        JSDataRequest jsDataRequest = JS.createJsType(JSDataRequest.class);
        jsDataRequest.setIndex(firstRowIndex);
        jsDataRequest.setCount(numberOfRows);
        jsDataRequest.setSortOrder(JS.prop(gridElement.getContainer(),
                "sortOrder"));

        gridElement.setLoadingDataClass(true);

        JS.exec(jsFunction, jsDataRequest, wrapCallback(callback));
    }

    private JavaScriptObject wrapCallback(
            final RequestRowsCallback<Object> callback) {
        return JS.wrapFunction(new Function() {
            @Override
            public Object f(Object p0, Object p1, Object p2) {
                List<Object> list = JS.asList((JavaScriptObject) p0);
                Double totalSize = (Double) p1;

                for (int i = 0; i < list.size(); i++) {
                    if (JS.isPrimitiveType(list.get(i))) {
                        list.set(i, new DataItemContainer(list.get(i)));
                    }
                }

                if (totalSize != null) {
                    setSize(totalSize.intValue());
                }

                if (callback != null) {
                    callback.onResponse(list, size());
                }

                gridElement.setLoadingDataClass(false);

                if (!initialRowSetReceived && !list.isEmpty()) {
                    initialRowSetReceived = true;
                    gridElement.updateWidth();
                }
                return null;
            }

        });
    }

}
