# Serialize

Serialize is a module for data serialization. Serialize supports data persistence 
so all types of objects including primitives can be serialized with this module. 
It consists of three main class, IOAdapter, DataReader/Writer, DataStore. There are IOAdapters for each data type and
it decides how data are to be encode and decoded. DataReader/Writer, with adapters, serialize and deserialize data
and decide in what format it has to be transformed. DataStore saves and loads data from file. 