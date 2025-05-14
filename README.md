### Running simulations

To run the simulation where we use a sequence for primary key generation:
```
mvn clean spring-boot:run -Dspring-boot.run.arguments="--experiment.simulation-type=sequence"
```

To run the simulation where we use a historized table structure:
```
mvn clean spring-boot:run -Dspring-boot.run.arguments="--experiment.simulation-type=historic"
```
