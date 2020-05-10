package cz.bezvadilna.prazskawc.adapter


import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import cz.bezvadilna.prazskawc.R
import cz.bezvadilna.prazskawc.api.formatDistanceNumber
import cz.bezvadilna.prazskawc.api.formatOpeningHours
import cz.bezvadilna.prazskawc.api.formatPriceNumber
import cz.bezvadilna.prazskawc.model.Point
import kotlinx.android.synthetic.main.adapter_item_point.view.*

class PointListAdapter(private val points: List<Point>, val context: Context) :
    RecyclerView.Adapter<PointListAdapter.PointHolder>() {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): PointHolder {
        val inflatedView =
            LayoutInflater.from(parent.context).inflate(R.layout.adapter_item_point, parent, false)
        return PointHolder(inflatedView)
    }

    override fun getItemCount(): Int {
        return points.size
    }

    override fun onBindViewHolder(holder: PointHolder, position: Int) {
        val itemPoint = points[position]
        holder.onBind(itemPoint, context)
    }

    class PointHolder(private val view: View) : RecyclerView.ViewHolder(view) {
        fun onBind(point: Point, context: Context) {
            view.textViewAddress.text = point.place.address
            view.textViewOpening.text = formatOpeningHours(point.place.openingHours, context)
            view.textViewPrice.text = formatPriceNumber(point.place.price, point.place.priceAlternate, context)
            view.textViewDistance.text = formatDistanceNumber(point.distance)
            val s = view.textViewOpening.text.toString() + " >" +
                    if (point.opened) {
                        context.getString(R.string.opening_hours_opened)
                    } else {
                        context.getString(R.string.opening_hours_closed)
                    }
            view.textViewOpening.text = s
        }
    }
}
