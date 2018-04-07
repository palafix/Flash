package nl.arnhem.flash.adapter

import android.content.Context
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
import nl.arnhem.flash.activities.HistoryActivity
import nl.arnhem.flash.model.HistoryModel
import nl.arnhem.flash.utils.L
import nl.arnhem.flash.utils.Prefs
import kotlin.properties.Delegates

/**
 * Created by meeera on 6/10/17.Edited by Robin Bouwman for this app on 1/1/18
 **/
class HistoryAdapter(var context: Context,
                     private var itemClick: OnItemClicked,
                     hisdata: OrderedRealmCollection<HistoryModel>,
                     autoUpdate: Boolean) :
        RealmRecyclerViewAdapter<HistoryModel,
                HistoryAdapter.MyViewHolder>(hisdata, autoUpdate) {

    var realm: Realm by Delegates.notNull()


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.history_item, parent, false)
        val myViewHolder = MyViewHolder(view)
        val card = view.findViewById<CardView>(R.id.historycard)
        val txt = view.findViewById<TextView>(R.id.txthistory)
        card?.setCardBackgroundColor(Prefs.notiColor.withAlpha(255))
        txt.setTextColor(Prefs.textColor)
        return myViewHolder
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.txt.text = data!![position].history
        holder.card.setOnClickListener({
            itemClick.onItemClick(data?.get(position)?.getHistory())
        })
        holder.delete.setImageResource(R.drawable.ic_delete_forever)
        holder.delete.setColorFilter(Prefs.textColor)
        holder.delete.setOnClickListener({
            realm.executeTransaction(({ _ ->
                L.v { "Delete from realm" }
                data!!.deleteFromRealm(position)
            }))
            reStart(context)
        })
    }

    private fun reStart(ctx: Context) {
        ctx as HistoryActivity
        ctx.recreate()
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        realm = Realm.getDefaultInstance()
    }

    override fun getItemCount(): Int {
        return (data?.size)!!.toInt()
    }

    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var txt = itemView.findViewById(R.id.txthistory) as TextView
        var card = itemView.findViewById(R.id.historycard) as CardView
        var delete = itemView.findViewById(R.id.delete) as ImageView
    }

    interface OnItemClicked {
        fun onItemClick(data: String?)
    }
}