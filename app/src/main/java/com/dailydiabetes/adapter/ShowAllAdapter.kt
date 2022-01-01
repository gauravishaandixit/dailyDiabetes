package com.dailydiabetes.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.dailydiabetes.R
import com.dailydiabetes.model.DiabetesValue
import java.util.*
import kotlin.collections.ArrayList

class ShowAllAdapter(
    private val diabetesValueList: ArrayList<DiabetesValue>,
    private val diabetesValueClickListener: DiabetesValueClickListener):
    RecyclerView.Adapter<ShowAllAdapter.MyViewHolder>() {

    private val SECOND_MILLIS = 1000
    private val MINUTE_MILLIS = 60 * SECOND_MILLIS
    private val HOUR_MILLIS = 60 * MINUTE_MILLIS
    private val DAY_MILLIS = 24 * HOUR_MILLIS

    class MyViewHolder(itemView: View,
                       private val diabetesValueClickListener: DiabetesValueClickListener):
        RecyclerView.ViewHolder(itemView), View.OnClickListener  {

        val value: TextView = itemView.findViewById(R.id.tvValue)
        val time: TextView = itemView.findViewById(R.id.tvTime)
        val date: TextView = itemView.findViewById(R.id.tvDate)

        init {
            itemView.setOnClickListener(this)
        }

        override fun onClick(p0: View?) {
            diabetesValueClickListener.onDiabetesValueClickListener(adapterPosition)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.diabetes_value_item,
            parent, false)

        return MyViewHolder(itemView, diabetesValueClickListener)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val currentDiabetesValue = diabetesValueList[position]

        holder.value.text = currentDiabetesValue.value.toString()
        holder.time.text = currentDiabetesValue.time
        holder.date.text = getTimeAgo(currentDiabetesValue.date)
    }

    fun getTimeAgo(time: Long): String {
        val diff = Calendar.getInstance().time.time - time
        return when {
            diff < MINUTE_MILLIS -> "moments ago"
            diff < 2 * MINUTE_MILLIS -> "a minute ago"
            diff < 60 * MINUTE_MILLIS -> "${diff / MINUTE_MILLIS} minutes ago"
            diff < 2 * HOUR_MILLIS -> "an hour ago"
            diff < 24 * HOUR_MILLIS -> "${diff / HOUR_MILLIS} hours ago"
            diff < 8 * 24 * HOUR_MILLIS -> "${diff / DAY_MILLIS} days ago"
            else -> "${Date(time).toLocaleString()}"
        }
    }

    override fun getItemCount(): Int {
        return diabetesValueList.size
    }

    interface DiabetesValueClickListener {
        fun onDiabetesValueClickListener(position: Int)
    }
}