package mobi.upod.app.data

import android.content.Context
import com.google.code.rome.android.repackaged.com.sun.syndication.feed.module.itunes.types.{Category => ITunesCategory}
import mobi.upod.app.R
import mobi.upod.data.Mapping._
import mobi.upod.data.{Mapping, MappingProvider}

final case class Category(name: String, subCategory: Option[String]) {

  def displayString(context: Context): Option[String] = {
    val resId = Category.StringResourceByKey.get(key)
    resId.map(context.getString)
  }

  def key = name + subCategory.map("_" + _).getOrElse("")

  def iTunesId: Option[Int] = Category.iTunesIdOf(key)

  override def toString = name + subCategory.map(" > " + _).getOrElse("")
}

object Category extends MappingProvider[Category] {
  import Mapping._

  def apply(category: String): Category = category.split('_').toList match {
    case name :: subCategory => Category(name, subCategory.headOption)
  }

  def apply(category: ITunesCategory): Category =
    apply(keyOf(category.getName), Option(category.getSubcategory).map(n => keyOf(n.getName)))

  private def keyOf(n: String): String =
    n.toLowerCase.replace('&', 'n').replace(' ', '-')

  def byITunesId(id: Int): Option[Category] =
    KeyByITunesId.get(id).map(apply)

  def iTunesIdOf(key: String): Option[Int] =
    ITunesIdByKey.get(key)

  val mapping = map(
    "name" -> string,
    "subCategory" -> optional(string)
  )(apply)(unapply)

  private val StringResourceByKey = Map(
    "arts" -> R.string.category_arts,
    "arts_design" -> R.string.category_arts_design,
    "arts_fashion-n-beauty" -> R.string.category_arts_fashion_n_beauty,
    "arts_food" -> R.string.category_arts_food,
    "arts_literature" -> R.string.category_arts_literature,
    "arts_performing-arts" -> R.string.category_arts_performing_arts,
    "arts_visual-arts" -> R.string.category_arts_visual_arts,
  
    "business" -> R.string.category_business,
    "business_business-news" -> R.string.category_business_business_news,
    "business_careers" -> R.string.category_business_careers,
    "business_investing" -> R.string.category_business_investing,
    "business_management-n-marketing" -> R.string.category_business_management_n_marketing,
    "business_shopping" -> R.string.category_business_shopping,
  
    "comedy" -> R.string.category_comedy,
  
    "education" -> R.string.category_education,
    "education_education" -> R.string.category_education_education,
    "education_education-technology" -> R.string.category_education_education_technology,
    "education_higher-education" -> R.string.category_education_higher_education,
    "education_k-12" -> R.string.category_education_k_12,
    "education_language-courses" -> R.string.category_education_language_courses,
    "education_training" -> R.string.category_education_training,
  
    "games-n-hobbies" -> R.string.category_games_n_hobbies,
    "games-n-hobbies_automotive" -> R.string.category_games_n_hobbies_automotive,
    "games-n-hobbies_aviation" -> R.string.category_games_n_hobbies_aviation,
    "games-n-hobbies_hobbies" -> R.string.category_games_n_hobbies_hobbies,
    "games-n-hobbies_other-games" -> R.string.category_games_n_hobbies_other_games,
    "games-n-hobbies_video-games" -> R.string.category_games_n_hobbies_video_games,
  
    "government-n-organizations" -> R.string.category_government_n_organizations,
    "government-n-organizations_local" -> R.string.category_government_n_organizations_local,
    "government-n-organizations_national" -> R.string.category_government_n_organizations_national,
    "government-n-organizations_non-profit" -> R.string.category_government_n_organizations_non_profit,
    "government-n-organizations_regional" -> R.string.category_government_n_organizations_regional,
  
    "health" -> R.string.category_health,
    "health_alternative-health" -> R.string.category_health_alternative_health,
    "health_fitness-n-nutrition" -> R.string.category_health_fitness_n_nutrition,
    "health_self-help" -> R.string.category_health_self_help,
    "health_sexuality" -> R.string.category_health_sexuality,
  
    "kids-n-family" -> R.string.category_kids_n_family,
  
    "music" -> R.string.category_music,
  
    "news-n-politics" -> R.string.category_news_n_politics,
  
    "religion-n-spirituality" -> R.string.category_religion_n_spirituality,
    "religion-n-spirituality_buddhism" -> R.string.category_religion_n_spirituality_buddhism,
    "religion-n-spirituality_christianity" -> R.string.category_religion_n_spirituality_christianity,
    "religion-n-spirituality_hinduism" -> R.string.category_religion_n_spirituality_hinduism,
    "religion-n-spirituality_islam" -> R.string.category_religion_n_spirituality_islam,
    "religion-n-spirituality_judaism" -> R.string.category_religion_n_spirituality_judaism,
    "religion-n-spirituality_other" -> R.string.category_religion_n_spirituality_other,
    "religion-n-spirituality_spirituality" -> R.string.category_religion_n_spirituality_spirituality,
  
    "science-n-medicine" -> R.string.category_science_n_medicine,
    "science-n-medicine_medicine" -> R.string.category_science_n_medicine_medicine,
    "science-n-medicine_natural-sciences" -> R.string.category_science_n_medicine_natural_sciences,
    "science-n-medicine_social-sciences" -> R.string.category_science_n_medicine_social_sciences,
  
    "society-n-culture" -> R.string.category_society_n_culture,
    "society-n-culture_history" -> R.string.category_society_n_culture_history,
    "society-n-culture_personal-journals" -> R.string.category_society_n_culture_personal_journals,
    "society-n-culture_philosophy" -> R.string.category_society_n_culture_philosophy,
    "society-n-culture_places-n-travel" -> R.string.category_society_n_culture_places_n_travel,
  
    "sports-n-recreation" -> R.string.category_sports_n_recreation,
    "sports-n-recreation_amateur" -> R.string.category_sports_n_recreation_amateur,
    "sports-n-recreation_college-n-high-school" -> R.string.category_sports_n_recreation_college_n_high_school,
    "sports-n-recreation_outdoor" -> R.string.category_sports_n_recreation_outdoor,
    "sports-n-recreation_professional" -> R.string.category_sports_n_recreation_professional,
  
    "technology" -> R.string.category_technology,
    "technology_gadgets" -> R.string.category_technology_gadgets,
    "technology_tech-news" -> R.string.category_technology_tech_news,
    "technology_podcasting" -> R.string.category_technology_podcasting,
    "technology_software-how-to" -> R.string.category_technology_software_how_to,
  
    "tv-n-film" -> R.string.category_tv_n_film
  )
  
  private val KeyByITunesId: Map[Int, String] = Map(
    1301 -> "arts",
    1402 -> "arts_design",
    1459 -> "arts_fashion-n-beauty",
    1306 -> "arts_food",
    1401 -> "arts_literature",
    1405 -> "arts_performing-arts",
    1406 -> "arts_visual-arts",

    1321 -> "business",
    1471 -> "business_business-news",
    1410 -> "business_careers",
    1412 -> "business_investing",
    1413 -> "business_management-n-marketing",
    1472 -> "business_shopping",

    1303 -> "comedy",

    1304 -> "education",
    1468 -> "education_education-technology",
    1416 -> "education_higher-education",
    1415 -> "education_k-12",
    1469 -> "education_language-courses",
    1470 -> "education_training",

    1323 -> "games-n-hobbies",
    1454 -> "games-n-hobbies_automotive",
    1455 -> "games-n-hobbies_aviation",
    1460 -> "games-n-hobbies_hobbies",
    1461 -> "games-n-hobbies_other-games",
    1404 -> "games-n-hobbies_video-games",

    1325 -> "government-n-organizations",
    1475 -> "government-n-organizations_local",
    1473 -> "government-n-organizations_national",
    1476 -> "government-n-organizations_non-profit",
    1474 -> "government-n-organizations_regional",

    1307 -> "health",
    1481 -> "health_alternative-health",
    1417 -> "health_fitness-n-nutrition",
    1420 -> "health_self-help",
    1421 -> "health_sexuality",

    1305 -> "kids-n-family",

    1310 -> "music",

    1311 -> "news-n-politics",

    1314 -> "religion-n-spirituality",
    1438 -> "religion-n-spirituality_buddhism",
    1439 -> "religion-n-spirituality_christianity",
    1463 -> "religion-n-spirituality_hinduism",
    1440 -> "religion-n-spirituality_islam",
    1441 -> "religion-n-spirituality_judaism",
    1464 -> "religion-n-spirituality_other",
    1444 -> "religion-n-spirituality_spirituality",

    1315 -> "science-n-medicine",
    1478 -> "science-n-medicine_medicine",
    1477 -> "science-n-medicine_natural-sciences",
    1479 -> "science-n-medicine_social-sciences",

    1324 -> "society-n-culture",
    1462 -> "society-n-culture_history",
    1302 -> "society-n-culture_personal-journals",
    1443 -> "society-n-culture_philosophy",
    1320 -> "society-n-culture_places-n-travel",

    1316 -> "sports-n-recreation",
    1467 -> "sports-n-recreation_amateur",
    1466 -> "sports-n-recreation_college-n-high-school",
    1456 -> "sports-n-recreation_outdoor",
    1465 -> "sports-n-recreation_professional",

    1318 -> "technology",
    1446 -> "technology_gadgets",
    1448 -> "technology_tech-news",
    1450 -> "technology_podcasting",
    1480 -> "technology_software-how-to",

    1309 -> "tv-n-film"
  )

  val ITunesIdByKey: Map[String, Int] = KeyByITunesId.map{case (id, key) => key -> id}
}

object Categories extends MappingProvider[Set[Category]] {

  override val mapping: Mapping[Set[Category]] = csv[Category](Category.apply, _.key)
}