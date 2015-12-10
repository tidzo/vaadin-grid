package com.vaadin.elements.grid.data;

import java.util.List;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.query.client.js.JsUtils;
import com.vaadin.client.data.CacheStrategy;
import com.vaadin.elements.common.js.JS;
import com.vaadin.elements.common.js.JSArray;
import com.vaadin.elements.common.js.JSFunction2;
import com.vaadin.elements.common.js.JSValidate;
import com.vaadin.elements.grid.GridElement;
import com.vaadin.elements.grid.config.JSDataRequest;
import com.vaadin.shared.ui.grid.Range;

/**
 * Datasource where requestRows() is delegated to a js native function
 */
public class GridJsFuncDataSource extends GridDataSource {

    private JSFunction2<JSDataRequest, JSFunction2<JSArray<?>, Double>> jsFunction;
    private boolean initialRowSetReceived;

    public GridJsFuncDataSource(
            JSFunction2<JSDataRequest, JSFunction2<JSArray<?>, Double>> jsFunction,
            GridElement grid) {
        super(grid);
        this.jsFunction = jsFunction;

        // Grid size might be 0 so we'll check it here and make an initial empty
        // data request to query for the size iff no size is given.
        Scheduler.get().scheduleFinally(() -> {
            if (size() == 0) {
                refreshItems();
            }
        });

        setCacheStrategy(new CacheStrategy.DefaultCacheStrategy() {

            @Override
            public int getMinimumCacheSize(int pageSize) {
                return super.getMinimumCacheSize(getFixedPageSize(pageSize));
            }

            @Override
            public int getMaximumCacheSize(int pageSize) {
                return super.getMaximumCacheSize(getFixedPageSize(pageSize));
            }

            private int getFixedPageSize(int defaultValue) {
                Integer dps = getDataPageSize();
                if (dps != null) {
                    if (getCachedRange().isEmpty()) {
                        return 0;
                    } else {
                        return dps;
                    }
                }
                return defaultValue;
            }

        });
    }

    private Integer getDataPageSize() {
        Object pageSize = JsUtils.prop(gridElement.getContainer(), "pageSize");
        return JSValidate.Integer.val(pageSize, null, null);
    }

    public void setJSFunction(
            JSFunction2<JSDataRequest, JSFunction2<JSArray<?>, Double>> jsFunction) {
        this.jsFunction = jsFunction;
        refreshItems();
        gridElement.getSelectionModel().reset();
    }

    @Override
    protected void requestRows(final int firstRowIndex, final int numberOfRows,
            final RequestRowsCallback<Object> callback) {

        JSDataRequest jsDataRequest = JS.createJsObject();
        jsDataRequest.setSortOrder(JsUtils.prop(gridElement.getContainer(),
                "sortOrder"));

        Integer pageSize = getDataPageSize();
        if (JS.isUndefinedOrNull(pageSize)) {
            // Not a paged request
            jsDataRequest.setIndex(firstRowIndex);
            jsDataRequest.setCount(numberOfRows);
            requestRows(jsDataRequest, callback);
        } else {
            int firstPage = (int) Math.floor(firstRowIndex / pageSize);
            int lastPage = (int) Math.floor((firstRowIndex + numberOfRows - 1)
                    / pageSize);

            if (firstPage == lastPage && numberOfRows == pageSize) {
                // This is a valid 1-page request
                jsDataRequest.setPage(firstPage);
                requestRows(jsDataRequest, callback);
            } else {
                // Make a new row request with a valid page range and ignore
                // this one.
                int page = pageCollidesCachedRange(lastPage, pageSize) ? lastPage
                        : firstPage;
                doRequest(Range.withLength(page * pageSize, pageSize));
            }
        }

    }

    private void requestRows(JSDataRequest jsDataRequest,
            RequestRowsCallback<Object> callback) {
        gridElement.setLoadingDataClass(true);

        jsFunction.f(jsDataRequest, (array, totalSize) -> {
            List<Object> list = JS.asList(array);
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
        });
    }

    private boolean pageCollidesCachedRange(int page, int pageSize) {
        Range cached = getCachedRange();
        Range range = Range.withLength(page * pageSize - 1, pageSize + 2);
        return cached.intersects(range);
    }
}
