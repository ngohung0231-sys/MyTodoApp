package com.hungday.mytodoapp.widget

import android.content.Intent
import android.widget.RemoteViewsService

class TodoWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return TodoWidgetFactory(this.applicationContext, intent)
    }
}