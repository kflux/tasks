/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.EditText;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ErrorCatchingEditText extends EditText {

    private static final Logger log = LoggerFactory.getLogger(ErrorCatchingEditText.class);

    public ErrorCatchingEditText(Context context, AttributeSet attrs,
            int defStyle) {
        super(context, attrs, defStyle);

    }

    public ErrorCatchingEditText(Context context, AttributeSet attrs) {
        super(context, attrs);

    }

    public ErrorCatchingEditText(Context context) {
        super(context);

    }

	@Override
	public int getExtendedPaddingTop() {
	    try {
            return super.getExtendedPaddingTop();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return 0;
        }
	}

	@Override
	public int getExtendedPaddingBottom() {
	    try {
            return super.getExtendedPaddingBottom();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return 0;
        }
	}

}
