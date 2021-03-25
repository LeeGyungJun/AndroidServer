package com.naver.test

import java.io.Serializable

class Item : Serializable{
    var itemid:Int? = null
    var itemname:String? = null
    var price:Int? = null
    var description:String? = null
    var pictureurl:String? = null
    var updatedate:String? = null
    override fun toString(): String {
        return "Item(itemid=$itemid, itemname=$itemname, price=$price, description=$description, updatedate=$updatedate)"
    }
}