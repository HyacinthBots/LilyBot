# Making changes to Lily's database system

With the introduction of migrations into 4.0.0 and a complete rewrite of the database, things are no longer quite as
simple as they were before. There are now extra steps to both adding and removing tables from the database.

### To add something to the database

* When adding to the database, you create a new data class in the `/database/entities` directory. This class should
  contain the fields you want to within this database table. The naming structure for these files is as follows:
  `<table_name>Data.kt`. The KDoc of these classes should contain information about what each parameter does/stores, a
  brief overview of the class as well as an `@since` annotation. No author field is required.

* Once you have created the data class, you next create a new class in the `/database/collections` directory. This class
  should implement `KordExKoinComponent` and contain the functions for interacting with the database table you just
  created. The naming structure for these files is as follows: `<table_name>Collection.kt`. The KDoc of these classes
  should contain a brief overview of the class, an `@since` annotation and `@see` annotations that point to the functions
  within the class. No author field is required. Each function should have a clear name and purpose. Standard names
  are `getXYZ`, `setXYZ`, `removeXYZ`. Further functions should be named appropriately. The majority of functions in these
  classes can be written as [Single-expression functions](https://kotlinlang.org/docs/functions.html#single-expression-functions).

* Now that you have created your database classes, you can set up these tables for the database to use. Go to the
  `/utils/_Utils.kt` file and find the `database` function. In said function there will be a `loadModule` DSL containing
  other database collections. To add the new table, simple add your new classes in the same was as the other tables.

```kotlin
loadModule {
    single { YourCollection() } bind YourCollection::class
}
```

* Finally, you can create a migration in the `/database/migrations` directory. There are two directories within this
  directory, `config` and `main`. The purpose of these is clear, you should know which database you're updating and place
  your migration in the appropriate directory. The naming structure for these files is as follows:
  `<directory>V<new version>.kt`, for example: `mainV24.kt`. Within the file you should add a function with the same
  name as your file name. These functions should take in a `CoroutineDatabase` as the parameter. To create your new table
  you should run `db.createCollection<YourData>("yourData")`. Navigate to the `/database/migrations/Migrator` object and
  find the function that links to the database you're updating. Add an entry to the `when` statement for the new version.

```kotlin
when (nextVersion) {
    1 -> ::mainV1
    // ...
    24 -> ::mainV24
    else -> break
}(db.mainDatabase)
```

* Your migration will run at next start up and the new database table is available for use.

### To remove something from the database

* When removing from the database, you should remove the data class and collection, that relate to the database table
  you are deleting, from the `/database/entities` and `/database/collections` directories.

* Next, you remove the table from the database function in the `/utils/_Utils.kt` file. To remove the table, simply
  remove the line that refers to your, now deleted collection.

* Finally, you can create a migration in the `/database/migrations` directory. There are two directories within this
  directory, `config` and `main`. The purpose of these is clear, you should know which database you're updating and place
  your migration in the appropriate directory. The naming structure for these files is as follows:
  `<directory>V<new version>.kt`, for example: `mainV25.kt`. Within the file you should add a function with the same name
  as your file name. These functions should take in a `CoroutineDatabase` as the parameter. To remove your table you
  should run `db.dropCollection("yourData")`. Navigate to the `/database/migrations/Migrator` object and find the function
  that links to the database you're updating. Add an entry to the `when` statement for the new version.

```kotlin
when (nextVersion) {
    1 -> ::mainV1
    // ...
    24 -> ::mainV24
    25 -> ::mainV25
    else -> break
}(db.mainDatabase)
```

* Your migration will run at next start up and the database table will be removed.

### To update a table in the database

* When updating a table in the database, you should update the data class and collection, that relate to the database
  table you are updating. You can find these files in the `/database/entities` and `/database/collections` directories.

* Once you have updated the data class and collection, you can create a migration in the `/database/migrations`
  directory. There are two directories within this directory, `config` and `main`. The purpose of these is clear, you
  should know which database you're updating and place your migration in the appropriate directory. The naming structure
  for these files is as follows: `<directory>V<new version>.kt`, for example: `mainV26.kt`. Within the file you should add
  a function with the same name as your file name. These functions should take in a `CoroutineDatabase` as the parameter.
  To update your table you should run `db.getCollection<YourData>("yourData").updateMany(* code to update *)`. Navigate to
  the `/database/migrations/Migrator` and find the function that links to the database you're updating. Add an entry to
  the `when` statement for the new version.

```kotlin
when (nextVersion) {
    1 -> ::mainV1
    // ...
    24 -> ::mainV24
    25 -> ::mainV25
    26 -> ::mainV26
    else -> break
}(db.mainDatabase)
```

* Your migration will run at next start up and the database table will be updated.
