### Running simulations

```
docker compose up -d
```

To run the simulation where we use a sequence for primary key generation:
```
mvn clean spring-boot:run -Dspring-boot.run.arguments="--experiment.simulation-type=sequence"
```

To run the simulation where we use a historized table structure:
```
mvn clean spring-boot:run -Dspring-boot.run.arguments="--experiment.simulation-type=historic"
```

To run the simulation where we use historized table structure with a many-to-many relationship:
```
mvn clean spring-boot:run -Dspring-boot.run.arguments="--experiment.simulation-type=historic_mtm"
```

Generate PlantUML diagrams
```
mvn plantuml:generate
```