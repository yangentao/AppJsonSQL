package dev.entao.app.jsonsql


import android.database.Cursor
import dev.entao.app.basic.newInstance
import dev.entao.app.json.*
import dev.entao.app.sql.firstRow

fun ysonToMap(yo: YsonObject, map: MutableMap<String, Any?>) {
    yo.forEach {
        val vv: Any? = when (val v = it.value) {
            is YsonNull -> null
            is YsonString -> v.data
            is YsonNum -> v.data
            is YsonObject -> v
            is YsonArray -> v
            else -> v
        }
        map[it.key] = vv
    }
}


//带下划线表示关闭Cursor
val Cursor.listYsonObject: List<YsonObject>
    get() {
        val ls = ArrayList<YsonObject>()
        this.use {
            while (this.moveToNext()) {
                ls += this.currentYsonObject
            }
        }
        return ls
    }

//带下划线表示关闭Cursor
val Cursor.firstYsonObject: YsonObject?
    get() {
        return firstRow {
            it.currentYsonObject
        }
    }

val Cursor.currentYsonObject: YsonObject
    get() {
        val map = YsonObject(32)
        val c = this
        val colCount = c.columnCount
        for (i in 0 until colCount) {
            val key = c.getColumnName(i)
            val v: YsonValue = when (c.getType(i)) {
                Cursor.FIELD_TYPE_NULL -> YsonNull.inst
                Cursor.FIELD_TYPE_INTEGER -> YsonNum(c.getLong(i))
                Cursor.FIELD_TYPE_FLOAT -> YsonNum(c.getDouble(i))
                Cursor.FIELD_TYPE_STRING -> YsonString(c.getString(i))
                Cursor.FIELD_TYPE_BLOB -> YsonBlob(c.getBlob(i))
                else -> YsonNull.inst
            }
            map.data[key] = v
        }
        return map
    }


//Person(val yo:YsonObject)
inline fun <reified T : Any> Cursor.listYsonModels(): List<T> {
    return this.toList {
        T::class.newInstance(YsonObject::class, it.currentYsonObject)
    }
}

//Person(val yo:YsonObject)
inline fun <reified T : Any> Cursor.firstYsonModel(): T? {
    return this.firstRow {
        val yo = it.currentYsonObject
        T::class.newInstance(YsonObject::class, yo)
    }
}


inline fun <reified T : Any> Cursor.toList(block: (Cursor) -> T): ArrayList<T> {
    val ls = ArrayList<T>(this.count + 8)
    this.use {
        while (it.moveToNext()) {
            ls += block(it)
        }
    }
    return ls
}

