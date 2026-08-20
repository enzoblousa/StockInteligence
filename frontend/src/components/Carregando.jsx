import Spinner from 'react-bootstrap/Spinner'

export default function Carregando() {
  return (
    <div className="d-flex justify-content-center my-4">
      <Spinner animation="border" role="status">
        <span className="visually-hidden">Carregando...</span>
      </Spinner>
    </div>
  )
}
