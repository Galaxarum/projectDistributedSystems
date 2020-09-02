docker network create 'storage-net' --driver bridge

MACHINES=('leader' 'replica1' 'replica2')
CLIENTS=('client_leader' 'client1' 'client2')

for machine in "${MACHINES[@]}" ; do
  docker rm -f "$machine"
  docker build -t "storage/$machine" -f "./application/$machine.dockerfile" "./application"
  docker run -d --network 'storage-net' --name "$machine" "storage/$machine"
  echo "---------------------"
done

for client in "${CLIENTS[@]}" ; do
  docker rm -f "$client"
  docker build -t "storage/$client" -f "./client/$client.dockerfile" "./client"
  docker run -i -t -d --network 'storage-net' --name "$client" "storage/$client"
  echo "---------------------"
done