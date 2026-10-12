package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.engine.MesocycleProgressionEngine
import com.example.engine.ProgressionModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        ExerciseEntity::class,
        MesocycleEntity::class,
        MesocycleWeekPlanEntity::class,
        WorkoutSessionEntity::class,
        WorkoutExerciseEntity::class,
        WorkoutSetEntity::class,
        ProgressiveOverloadLogEntity::class,
        ReadinessCheckinEntity::class,
        BodyMeasurementEntity::class,
        GymProfileEntity::class,
        UserVolumeConfigEntity::class,
        InternalScheduleEntity::class,
        TriggerLogEntity::class,
        ReminderLogEntity::class,
        WeeklySummaryEntity::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(ApexTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun apexDao(): ApexDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "apex_strength_database"
                )
                    .addCallback(DatabaseCallback(scope))
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateInitialData(database.apexDao())
                }
            }
        }
    }
}

suspend fun populateInitialData(dao: ApexDao) {
    // 1. Initial Exercises with Progressive Overload Metadata
    val exercises = listOf(
        ExerciseEntity(
            name = "Жим штанги лежачи",
            muscleGroup = "CHEST",
            secondaryMuscles = "TRICEPS,SHOULDERS",
            equipment = "BARBELL",
            movementPattern = "HORIZONTAL_PRESS",
            instructionsUk = "Ляжте на лаву, зведіть лопатки, упріться стопами в підлогу. Опускайте гриф до нижньої частини грудей під контролем, зберігаючи лікті під кутом ~45-70° до корпусу. Потужно витисніть вгору, не відриваючи таз.",
            jointSafetyUk = "Не розводьте лікті перпендикулярно корпусу (кут 90° перевантажує плечовий суглоб). Тримайте зап'ястя рівними.",
            substituteNames = "Жим гантелей лежачи, Жим у тренажері Chest Press, Відтискання на брусах",
            isCompound = true,
            defaultIncrementKg = 2.5,
            defaultTargetRepsMin = 6,
            defaultTargetRepsMax = 10,
            defaultTargetRir = 2,
            progressionType = "DOUBLE_PROGRESSION"
        ),
        ExerciseEntity(
            name = "Жим гантелей на похилій лаві (30°)",
            muscleGroup = "CHEST",
            secondaryMuscles = "SHOULDERS,TRICEPS",
            equipment = "DUMBBELL",
            movementPattern = "HORIZONTAL_PRESS",
            instructionsUk = "Встановіть лаву під кутом 30°. Утримуйте гантелі на рівні ключиць. Опускайте з глибоким розтягненням верхньої частини грудних м'язів, витискайте по дузі вгору без зіткнення гантелей.",
            jointSafetyUk = "Кут лави понад 30-45° переносить навантаження з грудей на передню дельту. Не прогинайте надмірно поперек.",
            substituteNames = "Жим штанги на похилій лаві, Жим у сміті на похилій лаві, Зведення у кросовері знизу-вгору",
            isCompound = true,
            defaultIncrementKg = 2.0,
            defaultTargetRepsMin = 8,
            defaultTargetRepsMax = 12,
            defaultTargetRir = 2,
            progressionType = "DOUBLE_PROGRESSION"
        ),
        ExerciseEntity(
            name = "Зведення рук у кросовері",
            muscleGroup = "CHEST",
            equipment = "CABLE",
            movementPattern = "ISOLATION",
            instructionsUk = "Встановіть блоки на рівні грудей або плечей. Зробіть крок уперед, трохи зігніть лікті. Зводьте рукоятки перед собою за рахунок скорочення грудних, затримуючись на 1 сек у піку.",
            jointSafetyUk = "Не випрямляйте руки в ліктях повністю під навантаженням. Не заводьте руки занадто далеко назад за спину.",
            substituteNames = "Тренажер метелик (Pec Deck), Розведення гантелей лежачи",
            isCompound = false,
            defaultIncrementKg = 2.5,
            defaultTargetRepsMin = 10,
            defaultTargetRepsMax = 15,
            defaultTargetRir = 1,
            progressionType = "METABOLIC_PUMP"
        ),
        ExerciseEntity(
            name = "Відтискання на брусах (акцент на груди)",
            muscleGroup = "CHEST",
            secondaryMuscles = "TRICEPS,SHOULDERS",
            equipment = "BODYWEIGHT",
            movementPattern = "HORIZONTAL_PRESS",
            instructionsUk = "Нахиліть корпус вперед на 30°, злегка розведіть лікті. Опускайтеся до відчуття комфортного розтягнення в грудях (кути в ліктях 90°), плавно повертайтеся вгору.",
            jointSafetyUk = "Уникайте надмірно глибокого опускання при нестабільності плечових суглобів. Уникайте ривків.",
            substituteNames = "Жим штанги лежачи вузьким хватом, Відтискання від підлоги з підвищенням",
            isCompound = true,
            defaultIncrementKg = 2.5,
            defaultTargetRepsMin = 8,
            defaultTargetRepsMax = 12,
            defaultTargetRir = 2
        ),
        ExerciseEntity(
            name = "Підтягування широким хватом",
            muscleGroup = "BACK",
            secondaryMuscles = "BICEPS,FOREARMS",
            equipment = "BODYWEIGHT",
            movementPattern = "VERTICAL_PULL",
            instructionsUk = "Візьміться за перекладину хватом ширше за плечі. З активного вису зведіть лопатки та тягніть груди до перекладини, спрямовуючи лікті до стегон. Опускайтеся повільно.",
            jointSafetyUk = "Не розгойдуйтеся (без кіпінгу). У нижній точці зберігайте напругу в плечах, не 'провисайте' на зв'язках.",
            substituteNames = "Тяга верхнього блоку до грудей, Підтягування в гравітроні",
            isCompound = true,
            defaultIncrementKg = 2.5,
            defaultTargetRepsMin = 6,
            defaultTargetRepsMax = 12,
            defaultTargetRir = 2
        ),
        ExerciseEntity(
            name = "Тяга верхнього блоку до грудей",
            muscleGroup = "BACK",
            secondaryMuscles = "BICEPS",
            equipment = "CABLE",
            movementPattern = "VERTICAL_PULL",
            instructionsUk = "Сядьте під валик, зафіксуйте стегна. Відхиліть торс на 10-15°. Тягніть рукоятку до верхньої лінії грудей, фокусуючись на русі ліктів вниз і назад.",
            jointSafetyUk = "Не тягніть блок за голову — це спричиняє травматичну ротацію плеча та шийного відділу хребта.",
            substituteNames = "Підтягування, Тяга верхнього блоку зворотним хватом",
            isCompound = true,
            defaultIncrementKg = 2.5,
            defaultTargetRepsMin = 8,
            defaultTargetRepsMax = 12,
            defaultTargetRir = 2
        ),
        ExerciseEntity(
            name = "Тяга штанги в нахилі до пояса",
            muscleGroup = "BACK",
            secondaryMuscles = "BICEPS,FOREARMS",
            equipment = "BARBELL",
            movementPattern = "HORIZONTAL_PULL",
            instructionsUk = "Кут нахилу спини ~45-70°, коліна злегка зігнуті, хребет нейтральний. Тягніть гриф уздовж стегон до низу живота, притискаючи лікті до корпусу.",
            jointSafetyUk = "Утримуйте спину ідеально рівною (без круглої спини). При болях у попереку переходьте на тягу з упором грудьми.",
            substituteNames = "Тяга гантелей з упором грудьми на похилій лаві, Тяга T-грифа",
            isCompound = true,
            defaultIncrementKg = 2.5,
            defaultTargetRepsMin = 8,
            defaultTargetRepsMax = 12,
            defaultTargetRir = 2
        ),
        ExerciseEntity(
            name = "Тяга горизонтального блоку до живота",
            muscleGroup = "BACK",
            secondaryMuscles = "BICEPS",
            equipment = "CABLE",
            movementPattern = "HORIZONTAL_PULL",
            instructionsUk = "Сядьте, злегка зігніть коліна. З рівною спиною тягніть V-подібну рукоятку до пупка, зводячи лопатки разом. На зворотному шляху дайте лопаткам розійтися для розтягнення найширших.",
            jointSafetyUk = "Не використовуйте інерцію розгойдуванням корпусу вперед-назад.",
            substituteNames = "Тяга штанги в нахилі, Тяга гантелі однією рукою",
            isCompound = true,
            defaultIncrementKg = 2.5,
            defaultTargetRepsMin = 8,
            defaultTargetRepsMax = 12,
            defaultTargetRir = 2
        ),
        ExerciseEntity(
            name = "Присідання зі штангою на спині",
            muscleGroup = "QUADS",
            secondaryMuscles = "GLUTES,HAMSTRINGS",
            equipment = "BARBELL",
            movementPattern = "SQUAT",
            instructionsUk = "Гриф на трапеціях, стопи трохи ширше плечей. Починайте рух одночасним згинанням колін та відведенням тазу. Присідайте до паралелі або нижче, коліна рухаються в площині носків.",
            jointSafetyUk = "Не допускайте завалювання колін всередину (вальгус) та відриву п'ят від підлоги.",
            substituteNames = "Гак-присідання, Жим ногами, Фронтальні присідання зі штангою",
            isCompound = true,
            defaultIncrementKg = 2.5,
            defaultTargetRepsMin = 6,
            defaultTargetRepsMax = 10,
            defaultTargetRir = 2,
            progressionType = "LINEAR_STRENGTH"
        ),
        ExerciseEntity(
            name = "Жим ногами в тренажері 45°",
            muscleGroup = "QUADS",
            secondaryMuscles = "GLUTES",
            equipment = "MACHINE",
            movementPattern = "SQUAT",
            instructionsUk = "Поставте стопи на середину платформи на ширині плечей. Опускайте платформу плавно до кута 90° у колінах. Тисніть усією стопою, не вставляючи коліна в «замок» у верхній точці.",
            jointSafetyUk = "Ніколи не випрямляйте коліна до блокування. Не відривайте поперек і таз від сидіння.",
            substituteNames = "Присідання зі штангою, Гак-присідання, Болгарські спліт-присідання",
            isCompound = true,
            defaultIncrementKg = 5.0,
            defaultTargetRepsMin = 10,
            defaultTargetRepsMax = 12,
            defaultTargetRir = 2
        ),
        ExerciseEntity(
            name = "Розгинання ніг у тренажері сидячи",
            muscleGroup = "QUADS",
            equipment = "MACHINE",
            movementPattern = "ISOLATION",
            instructionsUk = "Налаштуйте спинку так, щоб вісь обертання тренажера збігалася з колінним суглобом. Розгинайте гомілки, затримуючись на мить у піковому скороченні.",
            jointSafetyUk = "Уникайте ривків великої ваги; виконуйте підконтрольно для безпеки колінних зв'язок.",
            substituteNames = "Сісі-присідання, Випади на місці",
            isCompound = false,
            defaultIncrementKg = 5.0,
            defaultTargetRepsMin = 10,
            defaultTargetRepsMax = 15,
            defaultTargetRir = 1
        ),
        ExerciseEntity(
            name = "Румунська тяга зі штангою (RDL)",
            muscleGroup = "HAMSTRINGS",
            secondaryMuscles = "GLUTES,BACK",
            equipment = "BARBELL",
            movementPattern = "HIP_HINGE",
            instructionsUk = "Початкова позиція стоячи, гриф біля стегон. Злегка зігніть коліна та відводьте таз назад, ковзаючи штангою вздовж гомілок до середини гомілки. Відчуйте потужний натяг задньої поверхні стегна.",
            jointSafetyUk = "Рух здійснюється за рахунок тазу (hip hinge), а не згинання попереку! Спина залишається напруженою і прямою.",
            substituteNames = "Румунська тяга з гантелями, Гіперекстензія з акцентом на біцепс стегна",
            isCompound = true,
            defaultIncrementKg = 2.5,
            defaultTargetRepsMin = 8,
            defaultTargetRepsMax = 10,
            defaultTargetRir = 2
        ),
        ExerciseEntity(
            name = "Згинання ніг лежачи в тренажері",
            muscleGroup = "HAMSTRINGS",
            equipment = "MACHINE",
            movementPattern = "ISOLATION",
            instructionsUk = "Ляжте обличчям вниз, валик над п'ятами. Згинайте гомілки до торкання сідниць або максимально комфортно, утримуючи таз притиснутим до лави.",
            jointSafetyUk = "Не піднімайте таз над лавою під час зусилля — це переносить навантаження на поперековий відділ.",
            substituteNames = "Згинання ніг сидячи, Згинання ніг стоячи однією ногою",
            isCompound = false,
            defaultIncrementKg = 5.0,
            defaultTargetRepsMin = 10,
            defaultTargetRepsMax = 15,
            defaultTargetRir = 1
        ),
        ExerciseEntity(
            name = "Сідничний місток зі штангою (Hip Thrust)",
            muscleGroup = "GLUTES",
            secondaryMuscles = "HAMSTRINGS",
            equipment = "BARBELL",
            movementPattern = "HIP_HINGE",
            instructionsUk = "Верхня частина лопаток на лаві, гриф із захисною накладкою на складці тазу. Тисніть п'ятами в підлогу і виштовхуйте таз вгору до прямої лінії стегон і тулуба. Стискайте сідниці на 2 сек.",
            jointSafetyUk = "Уникайте перерозгинання попереку у верхній точці. Погляд спрямований вперед.",
            substituteNames = "Сідничний місток на одній нозі з гантеллю, Румунська тяга",
            isCompound = true,
            defaultIncrementKg = 5.0,
            defaultTargetRepsMin = 8,
            defaultTargetRepsMax = 12,
            defaultTargetRir = 2
        ),
        ExerciseEntity(
            name = "Жим штанги стоячи (Армійський жим)",
            muscleGroup = "SHOULDERS",
            secondaryMuscles = "TRICEPS",
            equipment = "BARBELL",
            movementPattern = "VERTICAL_PRESS",
            instructionsUk = "Хват трохи ширше плечей, лікті направлені вперед. Тисніть штангу вертикально вгору, напружуючи сідниці та прес для стабілізації корпусу. Повертайте голову вперед після проходження грифа.",
            jointSafetyUk = "Не прогинайте поперек дугою. Якщо бракує мобільності грудного відділу, виконуйте сидячи на лаві 75°.",
            substituteNames = "Жим гантелей сидячи, Жим штанги сидячи в Сміті",
            isCompound = true,
            defaultIncrementKg = 2.5,
            defaultTargetRepsMin = 6,
            defaultTargetRepsMax = 10,
            defaultTargetRir = 2
        ),
        ExerciseEntity(
            name = "Махи гантелями через сторони",
            muscleGroup = "SHOULDERS",
            equipment = "DUMBBELL",
            movementPattern = "ISOLATION",
            instructionsUk = "Трохи нахиліть корпус вперед, лікті ледь зігнуті. Піднімайте гантелі через сторони до рівня паралелі з підлогою, лікті ведуть рух. Контрольовано опускайте за 2-3 секунди.",
            jointSafetyUk = "Не використовуйте розгойдування тілом (чітінг). Не піднімайте лікті вище рівня плечей.",
            substituteNames = "Відведення руки в кросовері на середню дельту, Махи в тренажері",
            isCompound = false,
            defaultIncrementKg = 1.0,
            defaultTargetRepsMin = 12,
            defaultTargetRepsMax = 15,
            defaultTargetRir = 1
        ),
        ExerciseEntity(
            name = "Відведення на задню дельту в кросовері (Face Pull)",
            muscleGroup = "SHOULDERS",
            secondaryMuscles = "BACK",
            equipment = "CABLE",
            movementPattern = "HORIZONTAL_PULL",
            instructionsUk = "Встановіть канатну рукоятку на рівні очей. Тягніть канат до обличчя, розводячи кисті в сторони та ротуючи плече назовні. Пальці на рівні вух.",
            jointSafetyUk = "Відмінна вправа для стабілізаторів лопатки та ротаторної манжети. Уникайте висування голови вперед.",
            substituteNames = "Зворотні розведення в тренажері (Rear Delt Fly), Махи в нахилі з гантелями",
            isCompound = false,
            defaultIncrementKg = 2.5,
            defaultTargetRepsMin = 12,
            defaultTargetRepsMax = 15,
            defaultTargetRir = 1
        ),
        ExerciseEntity(
            name = "Підйом штанги на біцепс стоячи",
            muscleGroup = "BICEPS",
            secondaryMuscles = "FOREARMS",
            equipment = "BARBELL",
            movementPattern = "ISOLATION",
            instructionsUk = "Хват на ширині плечей, лікті притиснуті до боків. Згинайте руки в ліктях зусиллям двоголового м'яза плеча без розгойдування тулубом. Повільно опускайте вниз.",
            jointSafetyUk = "Тримайте лікті нерухомими, не виводьте їх далеко вперед під час підйому.",
            substituteNames = "Підйом гантелей із супінацією, Підйом на біцепс на нижньому блоці",
            isCompound = false,
            defaultIncrementKg = 2.5,
            defaultTargetRepsMin = 8,
            defaultTargetRepsMax = 12,
            defaultTargetRir = 1
        ),
        ExerciseEntity(
            name = "Підйом гантелей на похилій лаві (Incline Curl)",
            muscleGroup = "BICEPS",
            equipment = "DUMBBELL",
            movementPattern = "ISOLATION",
            instructionsUk = "Кут лави 45-60°. Руки звисають вільно, забезпечуючи глибоке розтягнення довгої головки біцепса. Згинайте руки із супінацією кисті (мізинець розвертається вгору).",
            jointSafetyUk = "Не відривайте спину і потилицю від лави. Підбирайте помірну вагу для чистої амплітуди.",
            substituteNames = "Підйом на лаві Скотта, Молоткові згинання з гантелями",
            isCompound = false,
            defaultIncrementKg = 2.0,
            defaultTargetRepsMin = 10,
            defaultTargetRepsMax = 12,
            defaultTargetRir = 1
        ),
        ExerciseEntity(
            name = "Французький жим з EZ-грифом лежачи",
            muscleGroup = "TRICEPS",
            equipment = "EZ_BAR",
            movementPattern = "ISOLATION",
            instructionsUk = "Ляжте на лаву, візьміть гриф вузьким хватом. Трохи відхиліть плечі назад у напрямку голови. Згинайте лише лікті, опускаючи гриф за верхівку голови, потім розгинайте руки.",
            jointSafetyUk = "Не розводьте лікті в сторони. Опускання за голову значно безпечніше для ліктьових сухожиль, ніж до лоба.",
            substituteNames = "Розгинання на трицепс на блоці, Французький жим із гантеллю сидячи",
            isCompound = false,
            defaultIncrementKg = 2.5,
            defaultTargetRepsMin = 8,
            defaultTargetRepsMax = 12,
            defaultTargetRir = 2
        ),
        ExerciseEntity(
            name = "Розгинання рук на блоці з канатом",
            muscleGroup = "TRICEPS",
            equipment = "CABLE",
            movementPattern = "ISOLATION",
            instructionsUk = "Візьміться за канат, зафіксуйте лікті біля ребер. Потужно розгинайте руки вниз, розводячи кінці канату в сторони у нижній точці для пікового скорочення.",
            jointSafetyUk = "Не дозволяйте ліктям рухатися вперед і назад під час повторів.",
            substituteNames = "Відтискання на брусах на трицепс, Жим донизу з прямою рукояткою",
            isCompound = false,
            defaultIncrementKg = 2.5,
            defaultTargetRepsMin = 10,
            defaultTargetRepsMax = 15,
            defaultTargetRir = 1
        ),
        ExerciseEntity(
            name = "Згинання зап'ясть зі штангою сидячи",
            muscleGroup = "FOREARMS",
            equipment = "BARBELL",
            movementPattern = "ISOLATION",
            instructionsUk = "Передпліччя лежать на лаві або колінах, кисті звисають з краю долонями вгору. Згинайте зап'ястя вгору, стискаючи м'язи передпліччя, під контролем опускайте вниз, злегка розкриваючи долоні.",
            jointSafetyUk = "Не робіть різких ривків; використовуйте помірну вагу та підконтрольну амплітуду для захисту зв'язок зап'ястя.",
            substituteNames = "Згинання зап'ясть з гантелями, Згинання зап'ясть на нижньому блоці"
        ),
        ExerciseEntity(
            name = "Розгинання зап'ясть зі штангою зворотним хватом",
            muscleGroup = "FOREARMS",
            equipment = "BARBELL",
            movementPattern = "ISOLATION",
            instructionsUk = "Передпліччя зафіксовані на лаві, долоні спрямовані вниз. Піднімайте кисті вгору силою розгиначів передпліччя, затримуючись на секунду у верхній точці.",
            jointSafetyUk = "Розгиначі слабші за згиначі — беріть легкий гриф або гантелі, уникайте перерозгинання суглоба.",
            substituteNames = "Розгинання зап'ясть на блоці, Згинання рук зі штангою зворотним хватом"
        ),
        ExerciseEntity(
            name = "Згинання рук зі штангою зворотним хватом",
            muscleGroup = "FOREARMS",
            equipment = "BARBELL",
            movementPattern = "ISOLATION",
            instructionsUk = "Візьміться за прямий або EZ-гриф хватом зверху (пронація) на ширині плечей. Піднімайте гриф до рівня грудей, зосереджуючись на роботі плечопроменевого м'яза (брахіорадіаліса).",
            jointSafetyUk = "Лікті тримайте притиснутими до корпусу, без розгойдування та ривків тулубом.",
            substituteNames = "Молоткові згинання з гантелями, Згинання Зоттмана"
        ),
        ExerciseEntity(
            name = "Молоткові згинання з гантелями (Хаммер)",
            muscleGroup = "FOREARMS",
            equipment = "DUMBBELL",
            movementPattern = "ISOLATION",
            instructionsUk = "Тримайте гантелі нейтральним хватом (долоні одна до одної). Згинайте передпліччя вгору без розгойдування ліктів, концентруючи навантаження на плечопроменевому м'язі (брахіорадіаліс) та силі хвату.",
            jointSafetyUk = "Не розгойдуйте корпус і не виводьте лікті вперед під час руху.",
            substituteNames = "Молоткові згинання на блоці з канатом, Згинання Зоттмана"
        ),
        ExerciseEntity(
            name = "Утримання вису на турніку (Сила хвату)",
            muscleGroup = "FOREARMS",
            equipment = "BODYWEIGHT",
            movementPattern = "ISOLATION",
            instructionsUk = "Повисність на перекладині прямим закритим хватом. Утримуйте положення на час, щільно стискаючи пальці та напружуючи м'язи передпліч.",
            jointSafetyUk = "Плечі та лопатки злегка активні, не провисайте пасивно на розслаблених суглобах.",
            substituteNames = "Прогулянка фермера з важкими гантелями, Згинання зап'ясть"
        ),
        ExerciseEntity(
            name = "Підйом на литки стоячи в тренажері",
            muscleGroup = "CALVES",
            equipment = "MACHINE",
            movementPattern = "ISOLATION",
            instructionsUk = "Подушечки стоп на краю платформи, коліна майже прямі. Опускайте п'яти максимально глибоко (пауза 2 сек для зняття ахіллової еластичності), потужно піднімайтеся на носки.",
            jointSafetyUk = "Пауза внизу обов'язкова для стимуляції гіпертрофії, інакше рух виконується на сухожильній пружині.",
            substituteNames = "Підйом на литки сидячи, Підйом на литки в тренажері для жиму ногами"
        ),
        ExerciseEntity(
            name = "Підйом ніг у висі на турніку",
            muscleGroup = "ABS",
            equipment = "BODYWEIGHT",
            movementPattern = "CORE",
            instructionsUk = "Висячи на турніку без розгойдування, підкручуйте таз вперед і піднімайте коліна (або прямі ноги) до рівня грудей, стискаючи прямий м'яз живота.",
            jointSafetyUk = "Рух має супроводжуватися підкручуванням тазу (posterior pelvic tilt), а не простим згинанням стегон клубово-поперековим м'язом.",
            substituteNames = "Скручування на блоці «Молитва», Планка з підтягуванням колін"
        ),
        ExerciseEntity(
            name = "Скручування на блоці «Молитва»",
            muscleGroup = "ABS",
            equipment = "CABLE",
            movementPattern = "CORE",
            instructionsUk = "Опустіться на коліна спиною до верхнього блоку, тримайте канат біля вух. Згинайте хребет за рахунок скорочення черевного преса, спрямовуючи лікті до стегон.",
            jointSafetyUk = "Не сідайте на п'яти і не згинайтеся в тазостегнових суглобах — згинатися повинен саме хребет.",
            substituteNames = "Підйом ніг у висі, Скручування на фітболі"
        )
    )
    dao.insertExercises(exercises)

    // 2. Default Gym Profile
    val mainGym = GymProfileEntity(
        name = "Основний зал (Повний арсенал)",
        isSelected = true,
        availableEquipment = "BARBELL,DUMBBELL,CABLE,MACHINE,BODYWEIGHT,EZ_BAR"
    )
    dao.insertGymProfile(mainGym)

    // 3. Initial Volume Configuration
    val defaultConfig = UserVolumeConfigEntity(
        id = 1,
        experienceLevel = "INTERMEDIATE",
        sleepFactor = "AVERAGE",
        nutritionFactor = "MAINTENANCE",
        stressFactor = "MODERATE",
        priorityMuscles = "CHEST,BACK"
    )
    dao.saveUserVolumeConfig(defaultConfig)

    // 4. Initial Active Mesocycle: "Гіпертрофія: Верх / Низ (Блок 1)"
    val mesoId = dao.insertMesocycle(
        MesocycleEntity(
            name = "Гіпертрофія: Верх / Низ (Блок 1)",
            totalWeeks = 5,
            currentWeek = 2,
            startDate = System.currentTimeMillis() - 7L * 24 * 3600 * 1000,
            isActive = true,
            isCompleted = false,
            programType = "Upper / Lower (4 дні)",
            priorityMuscles = "CHEST,BACK",
            progressionModel = "VOLUME_WAVE",
            deloadWeek = 5,
            startingRir = 3,
            peakRir = 0,
            weeklySetIncrement = 1,
            weeklyWeightIncrementPercent = 2.5f,
            goalDescriptionUk = "Хвилеподібна прогресія обсягу (MEV → MAV → MRV → Делоад)"
        )
    )

    // 4b. Seed MesocycleWeekPlanEntity rows for progressive overload structure
    val weekTargets = MesocycleProgressionEngine.calculateWeekProgressionTargets(5, ProgressionModel.VOLUME_WAVE)
    val weekPlanEntities = weekTargets.map { target ->
        val phaseCode = when {
            target.isDeload -> "DELOAD"
            target.weekNumber == 1 -> "INTRO_MEV"
            target.weekNumber == 2 -> "ACCUMULATION_MAV"
            target.weekNumber == 3 -> "OVERREACH_MRV"
            else -> "PEAK_INTENSITY"
        }
        MesocycleWeekPlanEntity(
            mesocycleId = mesoId,
            weekNumber = target.weekNumber,
            phaseCode = phaseCode,
            phaseNameUk = target.phaseNameUk,
            isDeload = target.isDeload,
            targetRir = target.targetRir,
            setsMultiplier = target.setsMultiplier,
            weightMultiplier = target.weightMultiplier,
            targetRepsMin = target.repsTargetMin,
            targetRepsMax = target.repsTargetMax,
            plannedTotalSets = (58 * target.setsMultiplier).toInt(),
            plannedTonnageKg = 24000.0 * target.setsMultiplier * target.weightMultiplier,
            coachGuidelineUk = target.coachGuidelineUk
        )
    }
    dao.insertMesocycleWeekPlans(weekPlanEntities)

    // Create workouts for this mesocycle
    val workout1Time = System.currentTimeMillis() - 2L * 24 * 3600 * 1000
    val workout1Id = dao.insertSession(
        WorkoutSessionEntity(
            mesocycleId = mesoId,
            weekNumber = 2,
            title = "Верх тіла А (Сила & Гіпертрофія)",
            dateTimestamp = workout1Time,
            durationSeconds = 3420,
            totalTonnageKg = 4685.0,
            isCompleted = true,
            readinessScore = 8.5f,
            notes = "Чудова сесія! Жим штанги йшов легко, запас 2 RIR.",
            totalWorkingSets = 8,
            averageRir = 1.75f,
            prCount = 2,
            progressionStatus = "PROGRESSED",
            previousSessionTonnageDeltaKg = 315.0
        )
    )

    // Exercise 1 in workout 1: Жим штанги лежачи
    val we1 = dao.insertWorkoutExercise(
        WorkoutExerciseEntity(
            sessionId = workout1Id,
            exerciseId = 1,
            exerciseName = "Жим штанги лежачи",
            muscleGroup = "CHEST",
            orderIndex = 1,
            targetSets = 3,
            targetRepsMin = 6,
            targetRepsMax = 10,
            targetWeightKg = 80.0,
            targetRir = 2,
            previousBestWeightKg = 77.5,
            previousBestReps = 8,
            overloadAchieved = true,
            progressionAction = "HOLD_WEIGHT_ADD_REPS"
        )
    )
    dao.insertSets(
        listOf(
            WorkoutSetEntity(workoutExerciseId = we1, setNumber = 1, weightKg = 80.0, reps = 8, targetRepsMin = 6, targetRepsMax = 10, rir = 2, isWarmup = false, isCompleted = true, isPersonalRecord = true, timestamp = workout1Time),
            WorkoutSetEntity(workoutExerciseId = we1, setNumber = 2, weightKg = 80.0, reps = 8, targetRepsMin = 6, targetRepsMax = 10, rir = 2, isWarmup = false, isCompleted = true, timestamp = workout1Time),
            WorkoutSetEntity(workoutExerciseId = we1, setNumber = 3, weightKg = 80.0, reps = 7, targetRepsMin = 6, targetRepsMax = 10, rir = 1, isWarmup = false, isCompleted = true, timestamp = workout1Time)
        )
    )

    // Exercise 2 in workout 1: Тяга штанги в нахилі
    val we2 = dao.insertWorkoutExercise(
        WorkoutExerciseEntity(
            sessionId = workout1Id,
            exerciseId = 7,
            exerciseName = "Тяга штанги в нахилі до пояса",
            muscleGroup = "BACK",
            orderIndex = 2,
            targetSets = 3,
            targetRepsMin = 8,
            targetRepsMax = 12,
            targetWeightKg = 75.0,
            targetRir = 2,
            previousBestWeightKg = 75.0,
            previousBestReps = 9,
            overloadAchieved = true,
            progressionAction = "HOLD_WEIGHT_ADD_REPS"
        )
    )
    dao.insertSets(
        listOf(
            WorkoutSetEntity(workoutExerciseId = we2, setNumber = 1, weightKg = 75.0, reps = 10, targetRepsMin = 8, targetRepsMax = 12, rir = 2, isWarmup = false, isCompleted = true, isPersonalRecord = true, timestamp = workout1Time),
            WorkoutSetEntity(workoutExerciseId = we2, setNumber = 2, weightKg = 75.0, reps = 10, targetRepsMin = 8, targetRepsMax = 12, rir = 2, isWarmup = false, isCompleted = true, timestamp = workout1Time),
            WorkoutSetEntity(workoutExerciseId = we2, setNumber = 3, weightKg = 75.0, reps = 9, targetRepsMin = 8, targetRepsMax = 12, rir = 1, isWarmup = false, isCompleted = true, timestamp = workout1Time)
        )
    )

    // Exercise 3 in workout 1: Жим гантелей на похилій лаві
    val we3 = dao.insertWorkoutExercise(
        WorkoutExerciseEntity(
            sessionId = workout1Id,
            exerciseId = 2,
            exerciseName = "Жим гантелей на похилій лаві (30°)",
            muscleGroup = "CHEST",
            orderIndex = 3,
            targetSets = 2,
            targetRepsMin = 8,
            targetRepsMax = 12,
            targetWeightKg = 28.0,
            targetRir = 2,
            previousBestWeightKg = 28.0,
            previousBestReps = 9,
            overloadAchieved = true,
            progressionAction = "HOLD_WEIGHT_ADD_REPS"
        )
    )
    dao.insertSets(
        listOf(
            WorkoutSetEntity(workoutExerciseId = we3, setNumber = 1, weightKg = 28.0, reps = 10, targetRepsMin = 8, targetRepsMax = 12, rir = 2, isWarmup = false, isCompleted = true, timestamp = workout1Time),
            WorkoutSetEntity(workoutExerciseId = we3, setNumber = 2, weightKg = 28.0, reps = 9, targetRepsMin = 8, targetRepsMax = 12, rir = 2, isWarmup = false, isCompleted = true, timestamp = workout1Time)
        )
    )

    // Seed initial Progressive Overload Logs for Workout 1
    dao.insertProgressiveOverloadLogs(
        listOf(
            ProgressiveOverloadLogEntity(
                exerciseId = 1,
                exerciseName = "Жим штанги лежачи",
                muscleGroup = "CHEST",
                sessionId = workout1Id,
                mesocycleId = mesoId,
                weekNumber = 2,
                timestamp = workout1Time,
                previousWeightKg = 77.5,
                newWeightKg = 80.0,
                previousReps = 8,
                newReps = 8,
                previousEstimated1RmKg = 77.5 * (1.0 + 8.0 / 30.0),
                newEstimated1RmKg = 80.0 * (1.0 + 8.0 / 30.0),
                previousExerciseTonnageKg = 1782.5,
                newExerciseTonnageKg = 1840.0,
                workingSetsCompleted = 3,
                avgRir = 1.67f,
                overloadType = "LOAD_PROGRESSION",
                nextRecommendedWeightKg = 80.0,
                nextRecommendedRepsMin = 6,
                nextRecommendedRepsMax = 10,
                summaryUk = "Прогресія ваги: +2.5 кг (77.5 → 80.0 кг × 8). Наступна ціль: 80.0 кг × 9 повт."
            ),
            ProgressiveOverloadLogEntity(
                exerciseId = 7,
                exerciseName = "Тяга штанги в нахилі до пояса",
                muscleGroup = "BACK",
                sessionId = workout1Id,
                mesocycleId = mesoId,
                weekNumber = 2,
                timestamp = workout1Time,
                previousWeightKg = 75.0,
                newWeightKg = 75.0,
                previousReps = 9,
                newReps = 10,
                previousEstimated1RmKg = 75.0 * (1.0 + 9.0 / 30.0),
                newEstimated1RmKg = 75.0 * (1.0 + 10.0 / 30.0),
                previousExerciseTonnageKg = 2025.0,
                newExerciseTonnageKg = 2175.0,
                workingSetsCompleted = 3,
                avgRir = 1.67f,
                overloadType = "REP_PROGRESSION",
                nextRecommendedWeightKg = 75.0,
                nextRecommendedRepsMin = 8,
                nextRecommendedRepsMax = 12,
                summaryUk = "Прогресія повторів: +1 повтор (75.0 кг × 9 → 10 повт.). Наступна ціль: 75.0 кг × 11 повт."
            ),
            ProgressiveOverloadLogEntity(
                exerciseId = 2,
                exerciseName = "Жим гантелей на похилій лаві (30°)",
                muscleGroup = "CHEST",
                sessionId = workout1Id,
                mesocycleId = mesoId,
                weekNumber = 2,
                timestamp = workout1Time,
                previousWeightKg = 28.0,
                newWeightKg = 28.0,
                previousReps = 9,
                newReps = 10,
                previousEstimated1RmKg = 28.0 * (1.0 + 9.0 / 30.0),
                newEstimated1RmKg = 28.0 * (1.0 + 10.0 / 30.0),
                previousExerciseTonnageKg = 504.0,
                newExerciseTonnageKg = 532.0,
                workingSetsCompleted = 2,
                avgRir = 2.0f,
                overloadType = "REP_PROGRESSION",
                nextRecommendedWeightKg = 28.0,
                nextRecommendedRepsMin = 8,
                nextRecommendedRepsMax = 12,
                summaryUk = "Прогресія повторів: +1 повтор (28.0 кг × 10). Наступна ціль: 28.0 кг × 11 повт."
            )
        )
    )

    // Today's scheduled workout: Низ тіла А
    val todayWorkoutId = dao.insertSession(
        WorkoutSessionEntity(
            mesocycleId = mesoId,
            weekNumber = 2,
            title = "Низ тіла А (Квадрицепси & Біцепс стегна)",
            dateTimestamp = System.currentTimeMillis(),
            durationSeconds = 0,
            totalTonnageKg = 0.0,
            isCompleted = false,
            readinessScore = null,
            notes = "",
            progressionStatus = "PLANNED"
        )
    )

    val weSquat = dao.insertWorkoutExercise(
        WorkoutExerciseEntity(
            sessionId = todayWorkoutId,
            exerciseId = 9,
            exerciseName = "Присідання зі штангою на спині",
            muscleGroup = "QUADS",
            orderIndex = 1,
            targetSets = 3,
            targetRepsMin = 6,
            targetRepsMax = 8,
            targetWeightKg = 100.0,
            targetRir = 2,
            previousBestWeightKg = 97.5,
            previousBestReps = 8,
            progressionAction = "INCREASE_WEIGHT"
        )
    )
    dao.insertSets(
        listOf(
            WorkoutSetEntity(workoutExerciseId = weSquat, setNumber = 1, weightKg = 100.0, reps = 8, targetRepsMin = 6, targetRepsMax = 8, rir = 2, isWarmup = false, isCompleted = false),
            WorkoutSetEntity(workoutExerciseId = weSquat, setNumber = 2, weightKg = 100.0, reps = 8, targetRepsMin = 6, targetRepsMax = 8, rir = 2, isWarmup = false, isCompleted = false),
            WorkoutSetEntity(workoutExerciseId = weSquat, setNumber = 3, weightKg = 100.0, reps = 8, targetRepsMin = 6, targetRepsMax = 8, rir = 2, isWarmup = false, isCompleted = false)
        )
    )

    val weRdl = dao.insertWorkoutExercise(
        WorkoutExerciseEntity(
            sessionId = todayWorkoutId,
            exerciseId = 12,
            exerciseName = "Румунська тяга зі штангою (RDL)",
            muscleGroup = "HAMSTRINGS",
            orderIndex = 2,
            targetSets = 3,
            targetRepsMin = 8,
            targetRepsMax = 10,
            targetWeightKg = 90.0,
            targetRir = 2,
            previousBestWeightKg = 90.0,
            previousBestReps = 9,
            progressionAction = "HOLD_WEIGHT_ADD_REPS"
        )
    )
    dao.insertSets(
        listOf(
            WorkoutSetEntity(workoutExerciseId = weRdl, setNumber = 1, weightKg = 90.0, reps = 10, targetRepsMin = 8, targetRepsMax = 10, rir = 2, isWarmup = false, isCompleted = false),
            WorkoutSetEntity(workoutExerciseId = weRdl, setNumber = 2, weightKg = 90.0, reps = 10, targetRepsMin = 8, targetRepsMax = 10, rir = 2, isWarmup = false, isCompleted = false),
            WorkoutSetEntity(workoutExerciseId = weRdl, setNumber = 3, weightKg = 90.0, reps = 10, targetRepsMin = 8, targetRepsMax = 10, rir = 2, isWarmup = false, isCompleted = false)
        )
    )

    val weLegPress = dao.insertWorkoutExercise(
        WorkoutExerciseEntity(
            sessionId = todayWorkoutId,
            exerciseId = 10,
            exerciseName = "Жим ногами в тренажері 45°",
            muscleGroup = "QUADS",
            orderIndex = 3,
            targetSets = 2,
            targetRepsMin = 10,
            targetRepsMax = 12,
            targetWeightKg = 180.0,
            targetRir = 2,
            previousBestWeightKg = 180.0,
            previousBestReps = 11,
            progressionAction = "HOLD_WEIGHT_ADD_REPS"
        )
    )
    dao.insertSets(
        listOf(
            WorkoutSetEntity(workoutExerciseId = weLegPress, setNumber = 1, weightKg = 180.0, reps = 12, targetRepsMin = 10, targetRepsMax = 12, rir = 2, isWarmup = false, isCompleted = false),
            WorkoutSetEntity(workoutExerciseId = weLegPress, setNumber = 2, weightKg = 180.0, reps = 12, targetRepsMin = 10, targetRepsMax = 12, rir = 2, isWarmup = false, isCompleted = false)
        )
    )

    val weLegCurls = dao.insertWorkoutExercise(
        WorkoutExerciseEntity(
            sessionId = todayWorkoutId,
            exerciseId = 13,
            exerciseName = "Згинання ніг лежачи в тренажері",
            muscleGroup = "HAMSTRINGS",
            orderIndex = 4,
            targetSets = 2,
            targetRepsMin = 10,
            targetRepsMax = 15,
            targetWeightKg = 45.0,
            targetRir = 2,
            previousBestWeightKg = 45.0,
            previousBestReps = 12,
            progressionAction = "HOLD_WEIGHT_ADD_REPS"
        )
    )
    dao.insertSets(
        listOf(
            WorkoutSetEntity(workoutExerciseId = weLegCurls, setNumber = 1, weightKg = 45.0, reps = 12, targetRepsMin = 10, targetRepsMax = 15, rir = 2, isWarmup = false, isCompleted = false),
            WorkoutSetEntity(workoutExerciseId = weLegCurls, setNumber = 2, weightKg = 45.0, reps = 12, targetRepsMin = 10, targetRepsMax = 15, rir = 2, isWarmup = false, isCompleted = false)
        )
    )

    // Initial Body Measurements
    dao.insertBodyMeasurement(
        BodyMeasurementEntity(
            dateTimestamp = System.currentTimeMillis() - 14L * 24 * 3600 * 1000,
            weightKg = 79.2,
            waistCm = 82.0,
            bicepsCm = 38.5,
            chestCm = 106.0,
            bodyFatPercent = 14.8
        )
    )
    dao.insertBodyMeasurement(
        BodyMeasurementEntity(
            dateTimestamp = System.currentTimeMillis() - 7L * 24 * 3600 * 1000,
            weightKg = 79.6,
            waistCm = 81.8,
            bicepsCm = 38.8,
            chestCm = 106.5,
            bodyFatPercent = 14.6
        )
    )

    // Initial 7-Day Readiness Check-in Trend
    val dayMillis = 24L * 3600 * 1000
    val checkinSeeds = listOf(
        ReadinessCheckinEntity(
            dateTimestamp = System.currentTimeMillis() - 6 * dayMillis,
            sleepQuality = 5,
            domsSoreness = 1,
            energyLevel = 5,
            motivation = 5,
            cnsScore = 9.4f,
            autoregulationMessage = "⚡ Відмінна готовність ЦНС (9.4/10)! Ідеальний день для спроби прогресії ваги або повторів."
        ),
        ReadinessCheckinEntity(
            dateTimestamp = System.currentTimeMillis() - 5 * dayMillis,
            sleepQuality = 4,
            domsSoreness = 3,
            energyLevel = 4,
            motivation = 4,
            cnsScore = 7.5f,
            autoregulationMessage = "✅ Оптимальний стан ЦНС (7.5/10). Працюйте за наміченим планом."
        ),
        ReadinessCheckinEntity(
            dateTimestamp = System.currentTimeMillis() - 4 * dayMillis,
            sleepQuality = 3,
            domsSoreness = 4,
            energyLevel = 3,
            motivation = 3,
            cnsScore = 5.6f,
            autoregulationMessage = "⚠️ Помірна втома ЦНС (5.6/10). Працюйте суворо в межах RIR 2-3."
        ),
        ReadinessCheckinEntity(
            dateTimestamp = System.currentTimeMillis() - 3 * dayMillis,
            sleepQuality = 2,
            domsSoreness = 4,
            energyLevel = 2,
            motivation = 3,
            cnsScore = 4.4f,
            autoregulationMessage = "🚨 Критична втома ЦНС (4.4/10). Рекомендовано знизити робочі ваги на 10%."
        ),
        ReadinessCheckinEntity(
            dateTimestamp = System.currentTimeMillis() - 2 * dayMillis,
            sleepQuality = 4,
            domsSoreness = 2,
            energyLevel = 4,
            motivation = 4,
            cnsScore = 8.1f,
            autoregulationMessage = "⚡ Відновлення пройшло успішно (8.1/10)! ЦНС готова до навантажень."
        ),
        ReadinessCheckinEntity(
            dateTimestamp = System.currentTimeMillis() - 1 * dayMillis,
            sleepQuality = 4,
            domsSoreness = 2,
            energyLevel = 4,
            motivation = 5,
            cnsScore = 8.5f,
            autoregulationMessage = "⚡ Відмінна готовність ЦНС (8.5/10)! Відновлення максимальне."
        ),
        ReadinessCheckinEntity(
            dateTimestamp = System.currentTimeMillis(),
            sleepQuality = 5,
            domsSoreness = 1,
            energyLevel = 4,
            motivation = 5,
            cnsScore = 8.8f,
            autoregulationMessage = "⚡ Відмінна готовність ЦНС (8.8/10)! Повна суперкомпенсація."
        )
    )
    for (c in checkinSeeds) {
        dao.insertReadinessCheckin(c)
    }
}
