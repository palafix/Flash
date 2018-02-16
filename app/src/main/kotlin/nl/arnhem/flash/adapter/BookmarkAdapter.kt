package nl.arnhem.flash.adapter

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.LightingColorFilter
import android.support.v7.widget.CardView
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import ca.allanwang.kau.utils.withAlpha
import io.realm.OrderedRealmCollection
import io.realm.Realm
import io.realm.RealmRecyclerViewAdapter
import nl.arnhem.flash.R
import nl.arnhem.flash.model.BookmarkModel
import nl.arnhem.flash.utils.Prefs
import kotlin.properties.Delegates


@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
/**
 * Created by meeera on 6/10/17.
 **/
class BookmarkAdapter(context: Context, var itemClick: BookmarkAdapter.onItemClicked, bookMarkData: OrderedRealmCollection<BookmarkModel>, autoUpdate: Boolean) : RealmRecyclerViewAdapter<BookmarkModel, BookmarkAdapter.MyViewHolder>(context, bookMarkData, autoUpdate) {

    var realm: Realm by Delegates.notNull()

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): MyViewHolder {
        val view = LayoutInflater.from(parent?.context).inflate(R.layout.bookmark_item, parent, false)
        val myViewHolder = MyViewHolder(view)
        val card = view.findViewById<CardView>(R.id.bookmarkcard)
        val numcard = view.findViewById<CardView>(R.id.numcard)
        val txt = view.findViewById<TextView>(R.id.txtbookmark)
        val num = view.findViewById<TextView>(R.id.numbers)
        card?.setCardBackgroundColor(Prefs.notiColor.withAlpha(255))
        numcard?.setCardBackgroundColor(Prefs.notiColor.withAlpha(255))
        txt?.setTextColor(Prefs.textColor)
        num?.setTextColor(Prefs.textColor)
        return myViewHolder
    }

    override fun onBindViewHolder(holder: MyViewHolder?, position: Int) {
        holder?.txt?.text = data?.get(position)?.title
        holder?.num?.text = (position + 1).toString()
        holder?.card?.setOnClickListener({
            itemClick.onItemClick(data?.get(position)?.bookMark)
        })
        holder?.delete?.setImageResource(R.drawable.ic_delete_forever)
        holder?.delete?.setColorFilter(Prefs.textColor)
        holder?.delete?.setOnClickListener({
            val removeFavorite = AlertDialog.Builder(context)
            removeFavorite.setTitle(R.string.deleted)
            removeFavorite.setIcon(R.drawable.ic_warning)
            removeFavorite.setMessage(context.resources.getString(R.string.deleted_dia) + data?.get(position)?.title + context.resources.getString(R.string.deleted_dia_2))
            removeFavorite.setPositiveButton(context.resources.getString(R.string.kau_ok), object : DialogInterface.OnClickListener {
                override fun onClick(dialog: DialogInterface, which: Int) {
                    realm.executeTransaction {
                        data!!.deleteFromRealm(position)
                    }
                }
            })
            removeFavorite.setNegativeButton(R.string.kau_cancel, null)
            removeFavorite.show()
                    .window.decorView.background.colorFilter = LightingColorFilter(Prefs.textColor, Prefs.headerColor)
            return@setOnClickListener
        })
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView?) {
        super.onAttachedToRecyclerView(recyclerView)
        realm = Realm.getDefaultInstance()
    }

    override fun getItemCount(): Int {
        return (data?.size)!!.toInt()
    }

    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var txt = itemView.findViewById<TextView>(R.id.txtbookmark) as TextView
        var num = itemView.findViewById<TextView>(R.id.numbers) as TextView
        var card = itemView.findViewById<CardView>(R.id.bookmarkcard) as CardView
        var numcard = itemView.findViewById<CardView>(R.id.numcard) as CardView
        var delete = itemView.findViewById<ImageView>(R.id.delete) as ImageView
    }

    interface onItemClicked {
        fun onItemClick(data: String?)
    }
}

