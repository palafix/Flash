package nl.arnhem.flash.adapter

import android.content.Context
import android.support.v7.widget.CardView
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import ca.allanwang.kau.utils.adjustAlpha
import ca.allanwang.kau.utils.lighten
import ca.allanwang.kau.utils.withAlpha
import ca.allanwang.kau.utils.withMinAlpha
import com.afollestad.materialdialogs.DialogAction
import com.afollestad.materialdialogs.GravityEnum
import com.afollestad.materialdialogs.MaterialDialog
import io.realm.OrderedRealmCollection
import io.realm.Realm
import io.realm.RealmRecyclerViewAdapter
import nl.arnhem.flash.R
import nl.arnhem.flash.activities.BookMarkActivity
import nl.arnhem.flash.model.BookmarkModel
import nl.arnhem.flash.utils.L
import nl.arnhem.flash.utils.Prefs
import kotlin.properties.Delegates


@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
/**
 * Created by meeera on 6/10/17.Edited by Robin Bouwman for this app on 1/1/18
 **/
class BookmarkAdapter(val context: Context,
                      private var itemClick: BookmarkAdapter.OnItemClicked,
                      bookMarkData: OrderedRealmCollection<BookmarkModel>?,
                      autoUpdate: Boolean) : RealmRecyclerViewAdapter<BookmarkModel,
        BookmarkAdapter.MyViewHolder>(bookMarkData, autoUpdate) {

    var realm: Realm by Delegates.notNull()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.bookmark_item, parent, false)
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


    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val dimmerTextColor = Prefs.textColor.adjustAlpha(0.8f)
        holder.txt.text = data!![position].title
        holder.num.text = (position + 1).toString()
        holder.card.setOnClickListener({
            itemClick.onItemClick(data!![position].bookMark)
        })
        holder.delete.setImageResource(R.drawable.ic_delete_forever)
        holder.delete.setColorFilter(Prefs.textColor)
        holder.delete.setOnClickListener({
            MaterialDialog.Builder(context)
                    .title(R.string.deleted)
                    .titleColor(Prefs.textColor)
                    .titleGravity(GravityEnum.CENTER)
                    .backgroundColor(Prefs.bgColor.lighten(0.1f).withMinAlpha(200))
                    .dividerColor(Prefs.notiColor)
                    .iconRes(R.drawable.ic_warning)
                    .content(context.resources.getString(R.string.deleted_dia) + data!![position]?.title + context.resources.getString(R.string.deleted_dia_2))
                    .contentColor(dimmerTextColor)
                    .widgetColor(dimmerTextColor)
                    .positiveText(R.string.kau_ok)
                    .positiveColor(Prefs.textColor)
                    .negativeText(R.string.kau_cancel)
                    .negativeColor(Prefs.textColor)
                    .btnSelector(R.drawable.md_btn_selector_custom, DialogAction.POSITIVE)
                    .buttonRippleColor(Prefs.iconColor)
                    .buttonsGravity(GravityEnum.CENTER)
                    .onPositive { _, _ ->
                        realm.executeTransaction(({ _ ->
                            L.v { "Delete from realm" }
                            data!!.deleteFromRealm(position)
                        }))
                        reStart(context)
                    }
                    .onNegative({ _, _ -> })
                    .show()
            return@setOnClickListener
        })
    }

    private fun reStart(ctx: Context) {
        ctx as BookMarkActivity
        ctx.recreate()
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        realm = Realm.getDefaultInstance()
    }

    override fun getItemCount(): Int {
        return (data!!.size)
    }

    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var txt = itemView.findViewById(R.id.txtbookmark) as TextView
        var num = itemView.findViewById(R.id.numbers) as TextView
        var card = itemView.findViewById(R.id.bookmarkcard) as CardView
        var numcard = itemView.findViewById(R.id.numcard) as CardView
        var delete = itemView.findViewById(R.id.delete) as ImageView
    }

    interface OnItemClicked {
        fun onItemClick(data: String?)
    }
}